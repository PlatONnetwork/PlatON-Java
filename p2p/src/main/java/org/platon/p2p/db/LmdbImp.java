package org.platon.p2p.db;

import org.apache.commons.lang3.tuple.Pair;
import org.lmdbjava.*;
import org.platon.p2p.common.LmdbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.allocateDirect;
import static org.lmdbjava.Env.create;
import static org.lmdbjava.EnvFlags.MDB_NOSUBDIR;
import static org.lmdbjava.KeyRange.closed;




public class LmdbImp implements DB{
    private static Logger logger = LoggerFactory.getLogger(LmdbImp.class);


    private Dbi<ByteBuffer> dbi;
    private Env<ByteBuffer> env;
    enum DataType {
        HASH((byte)1),
        HASHSIZE((byte)2),
        ZSET((byte)3),
        ZSCORE((byte)4),
        ZSIZE((byte)5);

        private final byte code;

        private DataType(byte code) {
            this.code = code;
        }

        public static DataType valueOf(byte code) {
            for (DataType mc : EnumSet.allOf(DataType.class))
                if (mc.code == code)
                    return mc;
            return null;
        }

        public byte getCode() {
            return code;
        }
    }

    private static class NameKeyValue {
        ByteBuffer name;
        ByteBuffer key;
        ByteBuffer value;
    }

    public LmdbImp(){


        Properties pro = System.getProperties();
        logger.trace("Staring LMDDB");

        String confPath = LmdbConfig.getInstance().getLmdbNativeLib();
        String workDir = System.getProperty("user.dir");
        String filePath = workDir.concat(File.separator).concat(confPath);
        System.out.println("LD_PATH:" + filePath);

        final File nativeLib = new File(filePath);
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
        if (nativeLib.isFile() && nativeLib.exists()){
            pro.setProperty("lmdbjava.native.lib", filePath);
        } else {
            if (!nativeLib.exists()) {
                if (isWindows) {
                    pro.setProperty("lmdbjava.native.lib", LmdbImp.class.getClass().getResource("/liblmdb.dll").getPath());
                } else {
                    pro.setProperty("lmdbjava.native.lib", LmdbImp.class.getClass().getResource("/liblmdb.so").getPath());
                }
            } else {
                if (isWindows) {
                    pro.setProperty("lmdbjava.native.lib", filePath + "\\liblmdb.dll");
                } else {
                    pro.setProperty("lmdbjava.native.lib", filePath + "/liblmdb.so");
                }
            }
        }

        String dbConfPath = LmdbConfig.getInstance().getLmdbDataFile();
        String dbFilePath = workDir.concat(File.separator).concat(dbConfPath);
        final File path = new File(dbFilePath);
        if(!path.exists()){
            try {
                path.getParentFile().mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        env = create(ByteBufferProxy.PROXY_OPTIMAL).setMaxReaders(500).open(path, MDB_NOSUBDIR );
        EnvInfo info = env.info();

        dbi= env.openDbi(LmdbConfig.getInstance().getLmdbName(), DbiFlags.MDB_CREATE);


    }

    @Override
    public void set(final byte[] key, final byte[] value) throws DBException {
        dbi.put(slice(key), slice(value));
    }

    @Override
    public byte[] get(final byte[] key) throws DBException {
        try(Txn<ByteBuffer> txn = env.txnRead()) {
            ByteBuffer buffer = dbi.get(txn, slice(key));
            if (buffer != null) {
                byte[] value = new byte[buffer.remaining()];
                buffer.get(value);
                return value;
            }
        }
        return null;
    }

    @Override
    public void del(final byte[] key) throws DBException {
        dbi.delete(slice(key));
    }

    @Override
    public void hset(final byte[] name, final byte[] key, final byte[] value) throws DBException{
        try(Txn<ByteBuffer> txn = env.txnWrite()) {
            ByteBuffer hkey = encodeHashNameKey(name, key);
            dbi.put(txn, hkey, slice(value));
            increSizeInteral(txn, encodeHashSizeKey(name));
            txn.commit();
        }
    }

    @Override
    public byte[] hget(final byte[] name, final byte[] key) throws DBException{
        ByteBuffer hkey = encodeHashNameKey(name, key);
        try(Txn<ByteBuffer> txn = env.txnRead()) {
            ByteBuffer buffer = dbi.get(txn, hkey);
            if (buffer != null) {
                byte[] value = new byte[buffer.remaining()];
                buffer.get(value);
                return value;
            }
        }

        return null;
    }

    @Override
    public List<Pair<byte[], byte[]>> hgetAll(final byte[] name) throws DBException {
        List<Pair<byte[], byte[]>> res = new ArrayList<>();

        KeyRange<ByteBuffer> keyRange = hashAtLeast(name);

        try(Txn<ByteBuffer> txn = env.txnRead()) {
            final CursorIterator<ByteBuffer> iter = dbi.iterate(txn, keyRange);


            for (CursorIterator.KeyVal<ByteBuffer> next : iter.iterable()){
                byte[] value = new byte[next.val().remaining()];
                NameKeyValue n  = decodeHashNameKey(next.key());

                byte[] key = new byte[n.key.remaining()];
                n.key.get(key);
                next.val().get(value);
                res.add(Pair.of(key, value));
            }
        }

        return res;
    }

    @Override
    public void hdel(final byte[] name, final byte[] key) throws DBException {
        try(Txn<ByteBuffer> txn = env.txnWrite()) {
            if (dbi.delete(txn, encodeHashNameKey(name, key))) {
                decrSizeInteral(txn, encodeHashSizeKey(name));
            }
            txn.commit();
        }
    }

    @Override
    public long hsize(final byte[] name) throws DBException {
        try(Txn<ByteBuffer> txn = env.txnRead()) {
            ByteBuffer size = dbi.get(txn, encodeHashSizeKey(name));
            if (size == null) {
                throw new DBException("get hash set size failed");
            }

            return size.getLong();
        }
    }

    @Override
    public void zset(final byte[] name, final byte[] key, final byte[] score) throws DBException {
        try(Txn<ByteBuffer> txn = env.txnWrite()) {
            ByteBuffer scoreKey = encodeZScoreKey(name, key, score);
            ByteBuffer setKey = encodeZSetKey(name, key);
            boolean needIncr = false;

            try(Txn<ByteBuffer> txnRead = env.txn(txn)) {
                ByteBuffer oldScore = dbi.get(txnRead, setKey);
                if (oldScore != null) {
                    if (oldScore.compareTo(slice(score)) == 0){
                        txnRead.close();
                        return;
                    }

                    dbi.delete(txn, encodeZScoreKey(name, key, oldScore.asCharBuffer().toString().getBytes()));
                } else {
                    needIncr = true;
                }
            }
            if (needIncr) {
                increSizeInteral(txn, encodeZSizeKey(name));
            }
            dbi.put(txn, scoreKey, slice("22".getBytes()));
            dbi.put(txn, setKey, slice(score));
            txn.commit();
        }
    }


    @Override
    public byte[] zget(final byte[] name, final byte[] key) throws DBException {
        try(Txn<ByteBuffer> txn = env.txnRead()) {
            ByteBuffer setKey = encodeZSetKey(name, key);
            ByteBuffer buf = dbi.get(txn, setKey);
            if (buf != null) {
                byte[] value = new byte[buf.remaining()];
                buf.get(value);
                return value;
            }
        }
        return null;
    }


    private void increSizeInteral(Txn<ByteBuffer> txn, ByteBuffer name) throws DBException {
        long size = 1;
        ByteBuffer oldSize = dbi.get(txn, name);
        if (oldSize != null && oldSize.hasRemaining()) {
            size = oldSize.getLong() + 1;
        }

        ByteBuffer newSize = allocateDirect(8);
        newSize.putLong(size);
        newSize.rewind();
        dbi.put(txn, name, newSize);

    }


    private void decrSizeInteral(Txn<ByteBuffer> txn,ByteBuffer name) throws DBException {

        ByteBuffer oldSize = dbi.get(txn, name);
        long size = 0;
        if (oldSize.hasRemaining() && oldSize.getLong() > 0) {
            size = ((ByteBuffer)oldSize.rewind()).getLong() - 1;
        } else {
            throw new DBException("decr size overflow");
        }

        ByteBuffer newSize = allocateDirect(8);
        newSize.putLong(size).rewind();
        dbi.put(txn, name, newSize);
    }

    @Override
    public void zdel(final byte[] name, final byte[] key) throws DBException {
        ByteBuffer delKey = encodeZSetKey(name, key);
        try(Txn<ByteBuffer> txn = env.txnWrite()) {

            ByteBuffer oldScore = dbi.get(txn, delKey);
            if (oldScore == null) {
                return;
            }


            dbi.delete(txn, delKey);
            dbi.delete(txn, encodeZScoreKey(name, key, oldScore.asCharBuffer().toString().getBytes()));
            decrSizeInteral(txn, encodeZSizeKey(name));
            txn.commit();
        }
    }

    @Override
    public long zrank(final byte[] name, final byte[] key) throws DBException {
        long rank = 0;
        ByteBuffer keyBuffer = allocate(key.length);
        keyBuffer.put(key).rewind();

        try(Txn<ByteBuffer> txn = env.txnRead()) {
            KeyRange<ByteBuffer> keyRange = zScoreAtLeast(name);

                final CursorIterator<ByteBuffer> iter = dbi.iterate(txn, keyRange);

                while (iter.hasNext()) {
                    CursorIterator.KeyVal<ByteBuffer> next = iter.next();
                    NameKeyValue res = decodeZScoreKey(next.key());
                    if (res.key.compareTo(keyBuffer) == 0) {
                        return rank;
                    } else {
                        rank += 1;
                    }
                }
        }
        throw new DBException("zrank failed");
    }

    @Override
    public long zsize(final byte[] name) throws DBException {
        try(Txn<ByteBuffer> txn = env.txnRead()) {
            ByteBuffer size = dbi.get(txn, encodeZSizeKey(name));
            if (size == null) {
                throw new DBException("get zset size failed");
            }
            return size.getLong();
        }
    }

    static private ByteBuffer slice(final byte[] value) {
        final ByteBuffer bb = allocateDirect(value.length);
        bb.put(value).flip();
        return bb;
    }


    public static  KeyRange<ByteBuffer> hashAtLeast(byte[] name) {
        ByteBuffer startBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length);

        ByteBuffer stopBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length + 1);

        startBuffer.put(DataType.HASH.getCode());
        startBuffer.putShort((short) name.length);
        startBuffer.put(name);
        startBuffer.rewind();
        stopBuffer.put(DataType.HASH.getCode());
        stopBuffer.putShort((short) name.length);
        stopBuffer.put(name);
        stopBuffer.put((byte) 0xFF);
        stopBuffer.rewind();
        return closed(startBuffer, stopBuffer);
    }


    static private ByteBuffer encodeHashNameKey(byte[] name, byte[] key) throws DBException {
        if (name.length == 0 || key.length == 0) {
            throw new DBException("hash name key encode failed");
        }
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length + key.length);

        
        byteBuffer.put(DataType.HASH.getCode());
        byteBuffer.putShort((short) name.length);
        byteBuffer.put(name);
        byteBuffer.put(key);
        byteBuffer.rewind();
        return byteBuffer;
    }


    static private NameKeyValue decodeHashNameKey(ByteBuffer buf) throws DBException {
        NameKeyValue res = new LmdbImp.NameKeyValue();
        DataType type = DataType.valueOf(buf.get());

        if (type == DataType.HASH) {
            short nameLen = buf.getShort();
            byte[] byteName = new byte[nameLen];
            buf.get(byteName, 0, nameLen);
            byte[] byteKey = new byte[buf.remaining()];
            buf.get(byteKey);

            res.name = allocate(byteName.length);
            res.key = allocate(byteKey.length);
            res.name.put(byteName).rewind();
            res.key.put(byteKey).rewind();
        } else {
            throw new DBException("hash name key decode failed");
        }
        return res;
    }
    static private ByteBuffer encodeHashSizeKey(byte[] name) throws DBException {
        if (name.length == 0) {
            throw new DBException("hash set key encode failed");
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length);

        
        byteBuffer.put(DataType.HASHSIZE.getCode());
        byteBuffer.putShort((short) name.length);
        byteBuffer.put(name);
        byteBuffer.rewind();

        return byteBuffer;
    }
































    static private ByteBuffer encodeZScoreKey(byte[] name, byte[] key, byte[] score) throws DBException {
        if (name.length == 0 || key.length == 0 || score.length == 0 || score.length > BigIntegerLength) {
            throw new DBException("zscore key encode failed");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length + 21  + key.length);

        BigInteger scoreBigInt = new BigInteger(score);
        
        byteBuffer.put(DataType.ZSCORE.getCode());
        byteBuffer.putShort((short) name.length);
        byteBuffer.put(name);


        if (scoreBigInt.signum() < 0) {
            byteBuffer.put((byte) '-');
        } else {
            byteBuffer.put((byte) '=');
        }

        if (score.length < BigIntegerLength) {
            byteBuffer.put(new byte[BigIntegerLength - score.length]);
        }

        byteBuffer.put(score);
        byteBuffer.put(key);
        byteBuffer.rewind();
        return byteBuffer;
    }

    static private NameKeyValue decodeZScoreKey(ByteBuffer buf) throws DBException {
        NameKeyValue res = new NameKeyValue();
        DataType type = DataType.valueOf(buf.get(0));
        if (type == DataType.ZSCORE) {
            buf.get();
            short nameLen = buf.getShort();
            byte[] byteName = new byte[nameLen];
            buf.get(byteName, 0, nameLen);

            byte signum = buf.get();

            byte[] byteScore = new byte[BigIntegerLength];
            buf.get(byteScore);

            byte[] byteKey = new byte[buf.remaining()];
            buf.get(byteKey);

            res.name = allocate(byteName.length);
            res.key = allocate(byteKey.length);
            res.value = allocate(byteScore.length);
            res.name.put(byteName).rewind();
            res.key.put(byteKey).rewind();
            res.value.put(byteScore).rewind();
        } else {
            throw new DBException("zscore key decode failed");
        }
        return res;
    }

    static private KeyRange<ByteBuffer> zScoreAtLeast(byte[] name) {
        ByteBuffer startBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length);
        ByteBuffer stopBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length+1);
        startBuffer.put(DataType.ZSCORE.getCode());
        startBuffer.putShort((short) name.length);
        startBuffer.put(name);
        startBuffer.rewind();

        stopBuffer.put(DataType.ZSCORE.getCode());
        stopBuffer.putShort((short) name.length);
        stopBuffer.put(name);
        stopBuffer.put((byte)0xFF);
        stopBuffer.rewind();
        return closed(startBuffer, stopBuffer);
    }

    static private ByteBuffer encodeZSetKey(byte[] name, byte[] key) throws DBException {
        if (name.length == 0 || key.length == 0) {
            throw new DBException("zset key encode failed");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length + key.length);

        
        byteBuffer.put(DataType.ZSET.getCode());
        byteBuffer.putShort((short) name.length);
        byteBuffer.put(name);
        byteBuffer.put(key);
        byteBuffer.rewind();

        return byteBuffer;
    }

    static private NameKeyValue decodeZSetKey(ByteBuffer buf) throws DBException {
        NameKeyValue res = new NameKeyValue();
        DataType type = DataType.valueOf(buf.get(0));
        if (type == DataType.ZSET) {
            short nameLen = buf.getShort();
            byte[] byteName = new byte[nameLen];
            buf.get(byteName, 0, nameLen);
            int keyLen = buf.getInt();
            byte[] byteKey = new byte[keyLen];
            buf.get(byteKey);

            res.name = allocate(byteName.length);
            res.key = allocate(byteKey.length);
            res.name.put(byteName).rewind();
            res.key.put(byteKey).rewind();
        } else {
            throw new DBException("zset key encode failed");
        }
        return res;
    }

    static private ByteBuffer encodeZSizeKey(byte[] name) throws DBException {
        if (name.length == 0) {
            throw new DBException("zsize key encode failed");
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 + 2 + name.length);

        
        byteBuffer.put(DataType.ZSIZE.getCode());
        byteBuffer.putShort((short) name.length);
        byteBuffer.put(name);
        byteBuffer.rewind();

        return byteBuffer;
    }

    static private void decodeZSizeKey(ByteBuffer buf, ByteBuffer name) throws DBException {
        DataType type = DataType.valueOf(buf.get(0));
        if (type == DataType.ZSIZE) {
            short nameLen = buf.getShort();
            if (nameLen == 0) {
                throw new DBException("zsize name too short");
            }
            byte[] byteName = new byte[nameLen];
            buf.get(byteName, 0, nameLen);
            name.put(byteName);
        } else {
            throw new DBException("zsize key encode failed");
        }
    }


}

package org.platon.storage.datasource.rocksdb;

import org.platon.common.AppenderName;
import org.platon.common.utils.FileUtil;
import org.platon.common.utils.Numeric;
import org.platon.storage.datasource.DbSettings;
import org.platon.storage.datasource.DbSource;
import org.platon.storage.utils.AutoLock;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RocksDBSource implements DbSource<byte[]> {

    private Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_DB);

    private ReadWriteLock dbLock = new ReentrantReadWriteLock();
    private AutoLock readLock = new AutoLock(dbLock.readLock());
    private AutoLock writeLock = new AutoLock(dbLock.writeLock());

    private DbSettings settings = DbSettings.DEFAULT;
    private String dbPath;
    private RocksDB db;
    private boolean alive;
    private String name;

    static {
        RocksDB.loadLibrary();
    }

    public RocksDBSource(String name, String dbPath) {
        this.name = name;
        this.dbPath = dbPath;
        if (logger.isDebugEnabled()) {
            logger.debug("create new rocksdb , db name: " + name + ", dbPath = " + dbPath);
        }
    }

    @PostConstruct
    @Override
    public void open() {
        open(DbSettings.DEFAULT);
    }

    @Override
    public void open(DbSettings settings) throws RuntimeException {
        this.settings = settings;
        try (AutoLock lock = writeLock.lock()) {

            if (isAlive()) {
                return;
            }
            if (name == null) {
                throw new NullPointerException("Required db name.");
            }
            try {

                Options options = new Options();
                options.setMaxOpenFiles(this.settings.getMaxOpenFiles());
                options.setIncreaseParallelism(settings.getMaxThreads());
                options.useFixedLengthPrefixExtractor(16);










                options.setCreateIfMissing(true);

                options.setSkipStatsUpdateOnDbOpen(true);

                options.setLevelCompactionDynamicLevelBytes(true);

                String pathStr = dbPath + File.separator + name;
                if(!new File(pathStr).exists()){
                    new File(pathStr).mkdirs();
                }
                db = RocksDB.open(options, dbPath + File.separator + name);
                logger.debug("db is open,db name:" + name);
                alive = true;
            } catch (Exception e) {
                logger.error("open db error,db name: " + name, e);
                throw new RuntimeException("init rocks db failed.");
            }
        }
    }

    @Override
    public void close() throws Exception {
        try (AutoLock lock = writeLock.lock()) {
            if (!isAlive()) {
                return;
            }
            db.close();
            if (logger.isDebugEnabled()) {
                logger.debug("db is close,db name:" + name);
            }
            alive = false;
        } catch (Exception e) {
            logger.error("close db error,db name: " + name, e);
        }
    }

    @Override
    public boolean isAlive() {
        return alive;
    }


    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
        try (AutoLock lock = writeLock.lock()) {
            traceLogPrintln("~> RocksDbSource.batchUpdate(): " + name + ", " + rows.size());
            WriteBatch batch = new WriteBatch();
            WriteOptions writeOptions = new WriteOptions();
            for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
                if (entry.getValue() == null) {
                    batch.delete(entry.getKey());
                } else {
                    batch.put(entry.getKey(), entry.getValue());
                }
            }
            db.write(writeOptions, batch);
            traceLogPrintln("<~ RocksDbSource.batchUpdate(): " + name + ", " + rows.size());
        } catch (Exception e) {
            logger.error("batchUpdate error,db name: " + name, e);
        }
    }

    @Override
    public void put(byte[] key, byte[] val) {
        try (AutoLock lock = readLock.lock()) {
            traceLogPrintln("~> RocksDbSource.put(): " + name + ", key: " + Numeric.toHexString(key) + ", " + (val == null ? "null" : val.length));
            if (val == null) {
                db.delete(key);
            } else {
                db.put(key, val);
            }
            traceLogPrintln("<~ RocksDbSource.put(): " + name + ", key: " + Numeric.toHexString(key) + ", " + (val == null ? "null" : val.length));
        } catch (Exception e) {
            logger.error("RocksDbSource put error, db name: " + name, e);
        }
    }

    @Override
    public byte[] get(byte[] key) {
        try(AutoLock lock = readLock.lock()) {
            traceLogPrintln("~> RocksDbSource.get(): " + name + ", key: " + Numeric.toHexString(key));
            byte[] res = db.get(key);
            traceLogPrintln("<~ RocksDbSource.get(): " + name + ", key: " + Numeric.toHexString(key) + ", " + (res == null ? "null" : res.length));
            return res;
        } catch (Exception e) {
            logger.error("RocksDbSource get error, db name: " + name, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(byte[] key) {
        try(AutoLock lock = readLock.lock()) {
            traceLogPrintln("~> RocksDbSource.delete(): " + name + ", key: " + Numeric.toHexString(key));
            db.delete(key);
            traceLogPrintln("<~ RocksDbDataSource.delete(): " + name + ", key: " + Numeric.toHexString(key));
        } catch (Exception e) {
            logger.error("delete error,db name: " + name, e);
        }
    }

    @Override
    public boolean flush() {
        return false;
    }

    @Override
    public Set<byte[]> keys() {
        try(AutoLock lock = readLock.lock()){
            traceLogPrintln("~> RocksDBStorage1.keys(): {}.", name);
            RocksIterator iterator = db.newIterator();
            Set<byte[]> result = new LinkedHashSet<>();
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                result.add(iterator.key());
            }
            traceLogPrintln("<~ RocksDBStorage1.keys(): {}, {} ", name, result.size());
            return result;
        }catch (Exception e){
            logger.error("RocksDBStorage1.keys(): throw exception.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {
        try {
            String pathStr = (getDbPath() + File.separator + name);
            traceLogPrintln("RocksDBStorage1.reset(): name is: {}, db abs path:{}", name, pathStr);
            close();
            FileUtil.recursiveDelete(pathStr);
            open(settings);
        }catch (Exception e){
            logger.error("RocksDBStorage1.reset() throw exception. ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] prefixLookup(byte[] key, int prefixBytes) throws RuntimeException {
        return new byte[0];
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private void traceLogPrintln(String format, Object... arguments){
        if (logger.isTraceEnabled()) {
            logger.trace(format, arguments);
        }
    }

    public String getDbPath() {
        return dbPath;
    }

}

package org.platon.core.datasource;

import org.platon.common.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.AbstractList;

public class DataSourceArray<V> extends AbstractList<V> {
    
    private ObjectDataSource<V> src;
    private static final byte[] SIZE_KEY = Hex.decode("FFFFFFFFFFFFFFFF");
    private int size = -1;

    public DataSourceArray(ObjectDataSource<V> src) {
        this.src = src;
    }

    public synchronized boolean flush() {
        return src.flush();
    }

    @Override
    public synchronized V set(int idx, V value) {
        if (idx >= size()) {
            setSize(idx + 1);
        }
        src.put(ByteUtil.intToBytes(idx), value);
        return value;
    }

    @Override
    public synchronized void add(int index, V element) {
        set(index, element);
    }

    @Override
    public synchronized V remove(int index) {
        throw new RuntimeException("Not supported yet.");
    }

    @Override
    public synchronized V get(int idx) {
        if (idx < 0 || idx >= size()) throw new IndexOutOfBoundsException(idx + " > " + size);
        return src.get(ByteUtil.intToBytes(idx));
    }

    @Override
    public synchronized int size() {
        if (size < 0) {
            byte[] sizeBB = src.getSource().get(SIZE_KEY);
            size = sizeBB == null ? 0 : ByteUtil.byteArrayToInt(sizeBB);
        }
        return size;
    }

    private synchronized void setSize(int newSize) {
        size = newSize;
        src.getSource().put(SIZE_KEY, ByteUtil.intToBytes(newSize));
    }
}

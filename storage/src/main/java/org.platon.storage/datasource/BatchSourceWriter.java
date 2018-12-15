package org.platon.storage.datasource;

import java.util.HashMap;
import java.util.Map;

public class BatchSourceWriter<Key, Value> extends AbstractChainedSource<Key, Value, Key, Value> {

    private Map<Key, Value> buffer = new HashMap<>();

    public BatchSourceWriter(BatchSource<Key, Value> src) {
        super(src);
    }

    private BatchSource<Key, Value> getBatchSource() {
        return (BatchSource<Key, Value>) getSource();
    }

    @Override
    public synchronized void delete(Key key) {
        buffer.put(key, null);
    }

    @Override
    public synchronized void put(Key key, Value val) {
        buffer.put(key, val);
    }

    @Override
    public Value get(Key key) {
        return getSource().get(key);
    }

    @Override
    public synchronized boolean flushImpl() {
        if (!buffer.isEmpty()) {
            getBatchSource().updateBatch(buffer);
            buffer.clear();
            return true;
        } else {
            return false;
        }
    }
}

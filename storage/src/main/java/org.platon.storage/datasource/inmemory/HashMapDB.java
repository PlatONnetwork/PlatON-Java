package org.platon.storage.datasource.inmemory;

import org.platon.common.utils.ByteComparator;
import org.platon.storage.datasource.DbSettings;
import org.platon.storage.datasource.DbSource;
import org.platon.storage.utils.AutoLock;
import org.platon.common.utils.ByteArrayMap;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HashMapDB<V> implements DbSource<V> {

    protected final Map<byte[], V> storage;
    private String name ;
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private AutoLock readLock = new AutoLock(rwLock.readLock());
    private AutoLock writeLock = new AutoLock(rwLock.writeLock());

    public HashMapDB(ByteArrayMap<V> storage) {
        this.storage = storage;
    }

    public HashMapDB() {
        this(new ByteArrayMap<V>());
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void open(DbSettings settings) throws RuntimeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Set<byte[]> keys() throws RuntimeException {
        try (AutoLock loc = readLock.lock()) {
            Set<byte[]> result = new LinkedHashSet<>();
            Set<byte[]> keys = getStorage().keySet();
            Iterator<byte[]> iterator = keys.iterator();
            while(iterator.hasNext()){
                result.add(iterator.next());
            }
            return result;
        }
    }

    @Override
    public void reset() {
        try (AutoLock lock = writeLock.lock()) {
            storage.clear();
        }
    }

    @Override
    public V prefixLookup(byte[] key, int prefixBytes) throws RuntimeException {
        try (AutoLock l = readLock.lock()) {
            for (Map.Entry<byte[], V> entry : getStorage().entrySet()) {
                if (ByteComparator.compareTo(key, 0, prefixBytes, entry.getKey(), 0, prefixBytes) == 0){
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "in-memory";
    }

    @Override
    public void updateBatch(Map<byte[], V> rows){
        try (AutoLock lock = writeLock.lock()) {
            for (Map.Entry<byte[], V> entry : rows.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void put(byte[] key, V value) {
        if (value == null) {
            delete(key);
        }else{
            try (AutoLock lock = writeLock.lock()) {
                getStorage().put(key, value);
            }
        }
    }

    @Override
    public V get(byte[] key) {
        try (AutoLock lock = readLock.lock()) {
            return getStorage().get(key);
        }
    }

    @Override
    public void delete(byte[] key) {
        try(AutoLock lock = writeLock.lock()){
            getStorage().remove(key);
        }
    }

    @Override
    public boolean flush() {
        return true;
    }

    public Map<byte[], V> getStorage() {
        return storage;
    }

}

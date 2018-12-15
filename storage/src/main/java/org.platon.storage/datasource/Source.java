package org.platon.storage.datasource;


public interface Source<K, V> {

    
    void put(K key, V value);

    
    V get(K key);

    
    void delete(K key);

    
    boolean flush();

}

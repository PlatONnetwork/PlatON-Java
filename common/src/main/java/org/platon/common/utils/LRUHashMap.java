package org.platon.common.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author - yanze
 * @date 2018/9/14 16:36
 * @version 0.0.1
 */
public class LRUHashMap<K,V> extends LinkedHashMap<K,V> {

	private long maxCapacity;

	private final Lock lock = new ReentrantLock();

	private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;


	private static final float DEFAULT_LOAD_FACTOR = 0.75f;


	public LRUHashMap(long size) {
		super(DEFAULT_INITIAL_CAPACITY,DEFAULT_LOAD_FACTOR,true);
		this.maxCapacity = size;
	}

	@Override
	public boolean containsValue(Object value) {
		return super.containsValue(value);
	}

	@Override
	public V get(Object key) {
		try {
			lock.lock();
			return super.get(key);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public V put(K key, V value) {
		try {
			lock.lock();
			return super.put(key, value);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		try {
			lock.lock();
			super.putAll(m);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public V remove(Object key) {
		try {
			lock.lock();
			return super.remove(key);
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
		return size() > this.maxCapacity;
	}

	public long getMaxCapacity() {
		return maxCapacity;
	}

	public void setMaxCapacity(long maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

}

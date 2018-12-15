package org.platon.storage.datasource.leveldb;

import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.platon.common.AppenderName;
import org.platon.common.utils.FileUtil;
import org.platon.common.utils.Numeric;
import org.platon.storage.datasource.DbSettings;
import org.platon.storage.datasource.DbSource;
import org.platon.storage.utils.AutoLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LevelDBSource implements DbSource<byte[]> {

    Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_DB);

    private ReadWriteLock dbLock = new ReentrantReadWriteLock();
    private AutoLock readLock = new AutoLock(dbLock.readLock());
    private AutoLock writeLock = new AutoLock(dbLock.writeLock());

    private DBFactory factory = Iq80DBFactory.factory;
    private DbSettings settings = DbSettings.DEFAULT;
    private String dbPath;
    private DB db;
    private boolean alive;
    private String name;

    public LevelDBSource(String name, String dbPath) {
        this.name = name;
        this.dbPath = dbPath;
        logger.debug("create new leveldb ,db name:{},db path:{}", name, dbPath);
    }

    @Override
    public void open() throws RuntimeException {
        open(settings);
    }

    @Override
    public void open(DbSettings settings) throws RuntimeException {
        this.settings = settings;
        dbLock.writeLock().lock();
        try {
            if (isAlive()) return;
            if (name == null) {
                throw new NullPointerException("db name is null");
            }

            Options options = new Options();

            options.createIfMissing(true);

            options.errorIfExists(false);



            options.maxOpenFiles(this.settings.getMaxOpenFiles());












            File dbFile = new File(dbPath + File.separator + name);
            try {
                db = factory.open(dbFile, options);
            } catch (IOException ioe) {

                if (ioe.getMessage().contains("Corruption:")) {
                    logger.warn("levelDB is corrupted, db name:" + name, ioe);
                    logger.info("Trying to repair, db name:" + name);
                    factory.repair(dbFile, options);
                    logger.info("Repair finished, db name:" + name);
                    db = factory.open(dbFile, options);
                } else {
                    throw ioe;
                }
            }
            alive = true;
        } catch (Exception e) {
            logger.error("open db error, db name:" + name, e);
            throw new RuntimeException(e.getMessage());
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws Exception {
        try (AutoLock lock = writeLock.lock()) {
            if (!isAlive()) {
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("For close db, name : {}", name);
            }
            db.close();
            alive = false;
        } catch (Exception e) {
            logger.error("close db error, db name:" + name, e);
            throw e;
        }
    }

    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
        try (AutoLock lock = writeLock.lock()) {
            if (logger.isTraceEnabled()) {
                logger.trace("~> LevelDBStorage.batchUpdate(): " + name + ", " + rows.size());
            }
            WriteBatch batch = db.createWriteBatch();
            for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
                if (entry.getValue() == null) {
                    batch.delete(entry.getKey());
                } else {
                    batch.put(entry.getKey(), entry.getValue());
                }
            }
            db.write(batch);
            if (logger.isTraceEnabled()) {
                logger.trace("<~ LevelDBStorage.batchUpdate(): " + name + ", " + rows.size());
            }
        }
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public Set<byte[]> keys() {
        try (AutoLock lock = readLock.lock()) {
            if (logger.isTraceEnabled()) {
                logger.trace("~> LevelDBStorage.keys(): " + name);
            }
            DBIterator iterator = db.iterator();
            Set<byte[]> result = new LinkedHashSet<>();
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                result.add(iterator.peekNext().getKey());
            }
            if (logger.isTraceEnabled()) {
                logger.trace("<~ LevelDBStorage.keys(): " + name);
            }
            return result;
        }
    }

    @Override
    public void reset() {
        try {
            String pathStr = (getDbPath() + File.separator + name);
            if (logger.isDebugEnabled()) {
                logger.debug("Do reset, name is: {}, db abs path: {}", name, pathStr);
            }
            close();
            FileUtil.recursiveDelete(pathStr);
            open(settings);
        } catch (Exception e) {
            logger.error("Do reset, throw exception.", e);
        }
    }

    @Override
    public byte[] prefixLookup(byte[] key, int prefixBytes) throws RuntimeException {
        throw new RuntimeException("The method of prefixLookup is not supported");
    }

    @Override
    public void put(byte[] key, byte[] val) {
        try (AutoLock lock = writeLock.lock()) {
            if (logger.isTraceEnabled()) {
                logger.trace("~> LevelDBStorage.put(): " + name + ", key: " + Numeric.toHexString(key) + ", " + (val == null ? "null" : val.length));
            }
            db.put(key, val);
            if (logger.isTraceEnabled()) {
                logger.trace("<~ LevelDBStorage.put(): " + name + ", key: " + Numeric.toHexString(key) + ", " + (val == null ? "null" : val.length));
            }
        }
    }

    @Override
    public byte[] get(byte[] key) {
        try (AutoLock lock = readLock.lock()) {
            if (logger.isTraceEnabled()) {
                logger.trace("~> LevelDBStorage.get(): " + name + ", key: " + Numeric.toHexString(key));
            }
            byte[] response = db.get(key);
            if (logger.isTraceEnabled()) {
                logger.trace("<~ LevelDBStorage.get(): " + name + ", key: " + Numeric.toHexString(key) + ", " + (response == null ? "null" : response.length));
            }
            return response;
        } catch (DBException e) {
            logger.error("~> LevelDBStorage.get() throw exception.", e);
        }
        return null;
    }

    @Override
    public void delete(byte[] key) {
        try (AutoLock lock = readLock.lock()) {
            if (logger.isTraceEnabled()) {
                logger.trace("~> LevelDBStorage.get(): " + name + ", key: " + Numeric.toHexString(key));
            }
            db.delete(key);
            if (logger.isTraceEnabled()) {
                logger.trace("<~ LevelDBStorage.get(): " + name + ", key: " + Numeric.toHexString(key));
            }
        }
        db.delete(key);
    }

    @Override
    public boolean flush() {

        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDbPath() {
        return dbPath;
    }

}

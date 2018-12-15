package org.platon.core.config;

import org.platon.common.AppenderName;
import org.platon.common.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;


public class DBVersionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DBVersionProcessor.class);

    public enum Behavior {
        EXIT, RESET, IGNORE
    }

    public void process(CoreConfig config) {

        String dbType = config.getKeyValueDataSource();
        if("leveldb".equals(dbType)){
            AppenderName.showWarn(
                    "Deprecated database engine detected.",
                    "'leveldb' support will be removed in one of the next releases.",
                    "thus it's strongly recommended to stick with 'rocksdb' instead.");
        }
        boolean reset = config.databaseReset();
        long resetBlock = config.databaseResetBlock();
        if(reset && 0 == resetBlock){
            FileUtil.recursiveDelete(config.databaseDir());
            putDatabaseVersion(config,config.databaseVersion());
            logger.info("To reset database success.");
        }

        final File versionFile = getDatabaseVersionFile(config);
        final Behavior behavior = Behavior.valueOf(
                config.getIncompatibleDatabaseBehavior()==null ? Behavior.EXIT.toString() : config.getIncompatibleDatabaseBehavior().toUpperCase()
        );


        final Integer expectedVersion = config.databaseVersion();
        if(isDatabaseDirectoryExists(config)){
            final Integer actualVersionRaw = getDatabaseVersion(versionFile);
            final boolean isVersionFileNotFound = (Integer.valueOf(-1)).equals(actualVersionRaw);
            final Integer actualVersion = isVersionFileNotFound ? 1 : actualVersionRaw;
            if(actualVersionRaw.equals(-1)){
                putDatabaseVersion(config,actualVersion);
            }

            if(actualVersion.equals(expectedVersion) || (isVersionFileNotFound && expectedVersion.equals(1))){
                logger.info("Database directory location: '{}', version: {}", config.databaseDir(), actualVersion);
            } else {

                logger.warn("Detected incompatible database version. Detected:{}, required:{}", actualVersion, expectedVersion);
                if(behavior == Behavior.EXIT){
                    AppenderName.showErrorAndExit(
                            "Incompatible database version " + actualVersion,
                            "Please remove database directory manually or set `database.incompatibleDatabaseBehavior` to `RESET`",
                            "Database directory location is " + config.databaseDir()
                    );
                }else if(behavior == Behavior.RESET){
                    boolean res = FileUtil.recursiveDelete(config.databaseDir());
                    if(!res){
                        throw new RuntimeException("Couldn't delete database dir : " + config.databaseDir());
                    }
                    putDatabaseVersion(config,config.databaseVersion());
                    logger.warn("Auto reset database directory according to flag.");
                }else {
                    logger.info("Continue working according to flag.");
                }
            }

        } else{
            putDatabaseVersion(config,config.databaseVersion());
            logger.info("Created database version file done.");
        }
    }

    public boolean isDatabaseDirectoryExists(CoreConfig config) {
        final File databaseFile = new File(config.databaseDir());
        return databaseFile.exists() && databaseFile.isDirectory() && databaseFile.list().length > 0;
    }


    public Integer getDatabaseVersion(File file) {
        if (!file.exists()) {
            return -1;
        }
        try (Reader reader = new FileReader(file)) {
            Properties prop = new Properties();
            prop.load(reader);
            return Integer.valueOf(prop.getProperty("databaseVersion"));
        } catch (Exception e) {
            logger.error("Problem reading current database version.", e);
            return -1;
        }
    }

    
    public void putDatabaseVersion(CoreConfig config, Integer version) {
        final File versionFile = getDatabaseVersionFile(config);
        versionFile.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(versionFile)) {
            Properties prop = new Properties();
            prop.setProperty("databaseVersion", version.toString());
            prop.store(writer, "Generated database version");
        } catch (Exception e) {
            throw new Error("Problem writing current database version ", e);
        }
    }

    
    private File getDatabaseVersionFile(CoreConfig config) {
        return new File(config.databaseDir() + "/version.properties");
    }
}
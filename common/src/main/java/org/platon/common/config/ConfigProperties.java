package org.platon.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import org.platon.common.AppenderName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfigProperties {

    private static Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    private static String configPath = null;

    private static final String configName = "platon.conf";

    public static ConfigProperties getInstance() {
        return ConfigProperties.SingletonContainer.instance;
    }

    private static class SingletonContainer {
        private static ConfigProperties instance = new ConfigProperties();
    }

    private Config config;

    private ClassLoader classLoader;

    public ConfigProperties() {
        this(ConfigFactory.empty());
    }

    public ConfigProperties(File configFile) {
        this(ConfigFactory.parseFile(configFile));
    }

    public ConfigProperties(String configResource) {
        this(ConfigFactory.parseResources(configResource));
    }

    public ConfigProperties(Config apiConfig) {
        this(apiConfig, ConfigProperties.class.getClassLoader());
    }

    public ConfigProperties(Config apiConfig, ClassLoader classLoader) {
        try {
            this.classLoader = classLoader;

            String fileName = getFullConfigFileName();
            File file = new File(fileName);
            if (file.exists()) {
                config = ConfigFactory.parseFile(file);
            } else {
                config = ConfigFactory.parseResources("./config/" + configName);
            }

            logger.debug("Config trace: " + config.root().render(ConfigRenderOptions.defaults().setComments(false).setJson(false)));
        } catch (Exception e) {
            logger.error("Can't read config.", e);
            throw new RuntimeException(e);
        }
    }

    private static String getFullConfigFileName() {
        return getConfigPath() + File.separator + configName;
    }

    public static void setConfigPath(String _configPath) {
        configPath = _configPath;
    }

    private static String getConfigPath() {
        if (configPath == null) {
            if (System.getProperty("config.dir") != null && !System.getProperty("config.dir").trim().isEmpty()) {
                return System.getProperty("config.dir");
            } else {
                return System.getProperty("user.dir") + File.separator + "config";
            }
        }
        return configPath;
    }

    public Config getConfig() {
        return config;
    }

}

package org.platon.core.keystore;

import org.platon.common.AppenderName;
import org.platon.common.utils.Numeric;
import org.platon.core.config.CoreConfig;
import org.platon.crypto.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FileSystemKeystore implements Keystore {

    private final static Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_KEY_STORE);

    @Autowired
    private CoreConfig config;

    public KeystoreFormat keystoreFormat = new KeystoreFormat();

    @Override
    public void removeKey(String address) {
        getFiles().stream()
                .filter(f -> hasAddressInName(address, f))
                .findFirst()
                .ifPresent(f -> f.delete());
    }

    @Override
    public void storeKey(ECKey key, String password) throws RuntimeException {

        final String address = Numeric.toHexString(key.getAddress());
        if (hasStoredKey(address)) {
            throw new RuntimeException("Keystore is already exist for address: " + address +
                    " Please remove old one first if you want to add with new password.");
        }

        final File keysFolder = getKeyStoreLocation().toFile();
        keysFolder.mkdirs();

        String content = keystoreFormat.toKeystore(key, password);
        storeRawKeystore(content, address);
    }

    @Override
    public void storeRawKeystore(String content, String address) throws RuntimeException {

        String fileName = "UTC--" + getISODate(System.currentTimeMillis()) + "--" + address;
        try {
            Files.write(getKeyStoreLocation().resolve(fileName), Arrays.asList(content));
        } catch (IOException e) {
            throw new RuntimeException("Problem storing key for address");
        }
    }

    @Override
    public String[] listStoredKeys() {
        return getFiles().stream()
                .filter(f -> !f.isDirectory())
                .map(f -> f.getName().split("--"))
                .filter(n -> n != null && n.length == 3)
                .map(a -> a[2])
                .toArray(size -> new String[size]);
    }

    @Override
    public ECKey loadStoredKey(String address, String password) throws RuntimeException {
        return getFiles().stream()
                .filter(f -> hasAddressInName(address, f))
                .map(f -> {
                    try {
                        return Files.readAllLines(f.toPath())
                                .stream()
                                .collect(Collectors.joining(""));
                    } catch (IOException e) {
                        throw new RuntimeException("Problem reading keystore file for address:" + address);
                    }
                })
                .map(content -> keystoreFormat.fromKeystore(content, password))
                .findFirst()
                .orElse(null);
    }

    private boolean hasAddressInName(String address, File file) {
        return !file.isDirectory() && file.getName().toLowerCase().endsWith("--" + address.toLowerCase());
    }

    @Override
    public boolean hasStoredKey(String address) {
        return getFiles().stream()
                .filter(f -> hasAddressInName(address, f))
                .findFirst()
                .isPresent();
    }

    private List<File> getFiles() {
        final File dir = getKeyStoreLocation().toFile();
        final File[] files = dir.listFiles();
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    private String getISODate(long milliseconds) {

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm'Z'");
        df.setTimeZone(tz);
        return df.format(new Date(milliseconds));
    }

    public Path getKeyStoreLocation() {

        String keystore = config.keystoreDir();

        if (!StringUtils.isEmpty(keystore)) {
            return Paths.get(keystore);
        }

        final String keystoreDir = "keystore";
        final String osName = System.getProperty("os.name").toUpperCase();

        if (osName.indexOf("WIN") >= 0) {
            return Paths.get(System.getenv("APPDATA") + "/Platon/" + keystoreDir);
        } else if (osName.indexOf("MAC") >= 0) {
            return Paths.get(System.getProperty("user.home") + "/Library/Platon/" + keystoreDir);
        } else {
            return Paths.get(System.getProperty("user.home") + "/.platon/" + keystoreDir);
        }
    }
}

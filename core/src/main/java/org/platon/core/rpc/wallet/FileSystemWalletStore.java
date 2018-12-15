package org.platon.core.rpc.wallet;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.platon.common.AppenderName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileSystemWalletStore {

    private final static Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_WALLET);
    
    private static String FILE_NAME = "wallet.json";

    public void toStore(List<WalletAddressItem> addresses) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            final String content = mapper.writeValueAsString(addresses);

            getKeyStoreLocation().toFile().mkdirs();
            Files.write(getKeyStoreLocation().resolve(FILE_NAME), Arrays.asList(content));
        } catch (Exception e) {
            logger.error("Problem storing data", e);
            throw new RuntimeException("Problem storing data. Message: " + e.getMessage(), e);
        }
    }

    public List<WalletAddressItem> fromStore() {
        final ObjectMapper mapper = new ObjectMapper();

        try {
            final File file = getKeyStoreLocation().resolve(FILE_NAME).toFile();

            final String content = Files.readAllLines(file.toPath())
                    .stream()
                    .collect(Collectors.joining(""));

            return mapper.readValue(content, new TypeReference<List<WalletAddressItem>>() {
            });
        } catch (NoSuchFileException e) {
            return Collections.EMPTY_LIST;
        } catch (IOException e) {
            throw new RuntimeException("Problem loading data. Message: " + e.getMessage(), e);
        }
    }

    protected Path getKeyStoreLocation() {
        return Paths.get("walletstore");
    }
}

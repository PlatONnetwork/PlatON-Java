package org.platon.core.keystore;

import org.junit.Test;
import org.platon.common.AppenderName;
import org.platon.common.utils.Numeric;
import org.platon.crypto.ECKey;
import org.platon.slice.message.response.StringArrayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;

public class KeystoreTest {

    private final static Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_KEY_STORE);
    
    Keystore fileSystemKeystore = new FileSystemKeystore() {

        Path keystorePath = null;
        {
            try {
                keystorePath = Files.createTempDirectory("keystore");
                logger.debug("keystore path : " + keystorePath.getFileName());
            } catch (IOException e) {
                e.printStackTrace();
            }
            keystoreFormat = new KeystoreFormat();
        }

        @Override
        public Path getKeyStoreLocation() {
            return keystorePath;
        }
    };

    @Test
    public void encodeDecode() throws Exception {
        final String password = "123";

        // generate new random private key
        final ECKey key = new ECKey();
        final String address = Numeric.toHexString(key.getAddress());

        fileSystemKeystore.removeKey(address);
        fileSystemKeystore.storeKey(key, password);

        ECKey loadedKey = fileSystemKeystore.loadStoredKey(address, password);

        fileSystemKeystore.removeKey(address);
    }

    @Test
    public void readCorrectKey() throws Exception {
        final String password = "123";
        final String address = "dc212a894a3575c61eadfb012c8db93923d806f5";

        fileSystemKeystore.removeKey(address);
        fileSystemKeystore.storeRawKeystore(CORRECT_KEY, address);

        final ECKey key = fileSystemKeystore.loadStoredKey(address, password);

        fileSystemKeystore.removeKey(address);

        assertNotNull(key);
    }

    @Test(expected = RuntimeException.class)
    public void readCorrectKeyWrongPassword() throws Exception {
        final String password = "1234";
        final String address = "dc212a894a3575c61eadfb012c8db93923d806f5";

        fileSystemKeystore.removeKey(address);
        fileSystemKeystore.storeRawKeystore(CORRECT_KEY, address);

        fileSystemKeystore.loadStoredKey(address, password);
    }

    @Test(expected = RuntimeException.class)
    public void importDuplicateKey() throws Exception {
        // generate new random private key
        final ECKey key = new ECKey();
        final String address = Hex.toHexString(key.getAddress());

        try {
            fileSystemKeystore.storeKey(key, address);
            fileSystemKeystore.storeKey(key, address);
        } finally {
            fileSystemKeystore.removeKey(address);
        }
    }


    @Test
    public void testStream() {
        String[] arr = new String[]{"a","b","c"};

        /*StringArrayResponse result = Arrays.stream(arr)
                .map(e -> {
                    return ByteString.copyFromUtf8(e);
                })
                .collect();
*/
        /*StringArrayResponse result = convertToResponse(arr);

        try {
            result.getDataList().asByteStringList()
                    .stream().
                    forEach(r -> {
                        try {
                            System.out.println(r.toString("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    });
        }catch (Exception e) {
            e.printStackTrace();
        }*/



        //System.out.println(result.toByteString().toString());
    }

    public StringArrayResponse convertToResponse(String[] element){
        StringArrayResponse.Builder builder = StringArrayResponse.newBuilder();
        Arrays.stream(element).forEach(str -> builder.addData(str));
        return builder.build();
    }

    private static String CORRECT_KEY = "{\"address\":\"a3ae7747a0690701cc84b453524fa7c99afcd8ac\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"4baa65c9e3438e28c657a3585c5b444746578a5b0f35e1816e43146a09dc9f94\",\"cipherparams\":{\"iv\":\"bca4d9a043c68a9b9d995492d29653f5\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"eadb4203d8618141268903a9c8c0ace4f45954e5c4679257b89b874f24b56ea3\"},\"mac\":\"b1b34957940158569ed129f9bb4373979c78748bdf6e33354bcc922d2a207efa\"},\"id\":\"c985b75c-01ef-49b7-b7f0-0c2db4c299bc\",\"version\":3}";
}

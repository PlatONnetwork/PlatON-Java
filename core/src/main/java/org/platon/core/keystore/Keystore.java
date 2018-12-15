package org.platon.core.keystore;

import org.platon.crypto.ECKey;

public interface Keystore {

    void removeKey(String address);

    void storeKey(ECKey key, String password) throws RuntimeException;

    void storeRawKeystore(String content, String address) throws RuntimeException;

    String[] listStoredKeys();

    ECKey loadStoredKey(String address, String password) throws RuntimeException;

    boolean hasStoredKey(String address);
}

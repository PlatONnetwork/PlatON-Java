package org.platon.crypto.domain;

/**
 * Temporary WalletJson Model
 * TODO : Wallet must rewrite
 */
public class WalletJson {

    private String address;

    private String publicKey;

    private String privateKey;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}

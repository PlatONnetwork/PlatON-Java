package org.platon.core.genesis;

import java.util.Map;

/**
 * GenesisJson
 *
 * @author yanze
 * @desc genesis json model
 * @create 2018-08-01 11:25
 **/
public class GenesisJson {

    private String autor;

    private String coinbase;

    private String timestamp;

    private String perentHash;

    private String energonCeiling;

    private String difficulty;

    private String consensus;

    private String extraData;

    private Map<String,AccountJson> accounts;

    private GenesisConfig jsonBlockchainConfig;

    public static class AccountJson {
        public String balance;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPerentHash() {
        return perentHash;
    }

    public void setPerentHash(String perentHash) {
        this.perentHash = perentHash;
    }

    public String getEnergonCeiling() {
        return energonCeiling;
    }

    public void setEnergonCeiling(String energonCeiling) {
        this.energonCeiling = energonCeiling;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setConsensus(String consensus) {
        this.consensus = consensus;
    }

    public String getConsensus() {
        return consensus;
    }

    public Map<String, AccountJson> getAccounts() {
        return accounts;
    }

    public void setAccounts(Map<String, AccountJson> accounts) {
        this.accounts = accounts;
    }

    public GenesisConfig getJsonBlockchainConfig() {
        return jsonBlockchainConfig;
    }

    public void setJsonBlockchainConfig(GenesisConfig jsonBlockchainConfig) {
        this.jsonBlockchainConfig = jsonBlockchainConfig;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public String getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(String coinbase) {
        this.coinbase = coinbase;
    }
}

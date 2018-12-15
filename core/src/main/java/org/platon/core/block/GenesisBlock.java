package org.platon.core.block;

import org.platon.common.utils.Numeric;
import org.platon.core.genesis.GenesisJson;
import org.platon.core.Repository;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GenesisBlock
 *
 * @author yanze
 * @desc genesis block
 * @create 2018-07-31 17:52
 **/
public class GenesisBlock extends Block {

    private Map<String, GenesisJson.AccountJson> accounts = new LinkedHashMap<>();

    public GenesisBlock(long timestamp, byte[] author, byte[] coinbase, byte[] parentHash, byte[] bloomLog, long number, BigInteger energonUsed, BigInteger energonCeiling, BigInteger difficulty, byte[] extraData) {
        super(timestamp, author, coinbase, parentHash, bloomLog, number, energonUsed, energonCeiling, difficulty, extraData);
    }

    public GenesisBlock(byte[] encodeBytes) {
        super(encodeBytes);
    }

    public Map<String, GenesisJson.AccountJson> getAccounts() {
        return accounts;
    }

    public void setAccounts(Map<String, GenesisJson.AccountJson> accounts) {
        this.accounts = accounts;
    }

    public void resolve(Repository repository) {
        for (String addrStr : accounts.keySet()) {
            byte[] address = Numeric.hexStringToByteArray(addrStr);
            BigInteger balance = new BigInteger(accounts.get(addrStr).balance);
            repository.createAccount(address);
            repository.addBalance(address, balance);
        }
    }
}

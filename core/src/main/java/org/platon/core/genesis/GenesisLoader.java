package org.platon.core.genesis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.platon.common.AppenderName;
import org.platon.common.utils.Numeric;
import org.platon.core.Repository;
import org.platon.core.block.GenesisBlock;
import org.platon.storage.trie.SecureTrie;
import org.platon.storage.trie.Trie;
import org.platon.storage.trie.TrieImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.Map;

/**
 * GenesisLoader
 *
 * @author yanze
 * @desc load genesis file to json
 * @create 2018-08-01 11:26
 **/
public class GenesisLoader {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    public static GenesisJson loadGenesisJson(String genesisName) {

        String fileUrl = Thread.currentThread().getContextClassLoader().getResource("config/" + genesisName).getPath();
        File file = new File(fileUrl);

        String genesisJsonStr;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            bufferedReader.close();
            genesisJsonStr = stringBuffer.toString();
            return parseGenesisJsonStr(genesisJsonStr);
        } catch (Exception e) {
            logger.error("~> loadGenesisJson error: ", e);
            return null;
        }
    }

    public static GenesisBlock loadGenesisBlock(Repository repository, GenesisJson genesisJson) {
        GenesisBlock genesisBlock = parseGenesisJson(genesisJson);
        genesisBlock.setAccounts(genesisJson.getAccounts());
        genesisBlock.getBlockHeader().setStateRoot(generateRootHash(genesisBlock.getAccounts()));
        genesisBlock.resolve(repository);
        return genesisBlock;
    }

    public static byte[] generateRootHash(Map<String, GenesisJson.AccountJson> accounts){
        Trie<byte[]> state = new SecureTrie();
        for (String key : accounts.keySet()) {
            state.put(Numeric.hexStringToByteArray(key), Numeric.hexStringToByteArray(accounts.get(key).balance));
        }
        return state.getRootHash();
    }

    private static GenesisBlock parseGenesisJson(GenesisJson genesisJson) {
        long timestamp = Long.parseLong(genesisJson.getTimestamp());
        byte[] author = Numeric.hexStringToByteArray(genesisJson.getAutor());
        byte[] parentHash = null != genesisJson.getPerentHash() ?
                Hex.decode(Numeric.cleanHexPrefix(genesisJson.getPerentHash())) : new byte[]{};
        BigInteger energonCeiling = new BigInteger(
                genesisJson.getEnergonCeiling() != null ? Numeric.cleanHexPrefix(genesisJson.getEnergonCeiling()) : "0");

        BigInteger difficultyBI = new BigInteger(
                null != genesisJson.getDifficulty() ? Numeric.cleanHexPrefix(genesisJson.getDifficulty()) : "0");
        byte[] extraData = genesisJson.getExtraData() != null ?
                Hex.decode(Numeric.cleanHexPrefix(genesisJson.getExtraData())) : new byte[]{};
        byte[] coinbase = Numeric.hexStringToByteArray(genesisJson.getCoinbase());
        GenesisBlock genesisBlock = new GenesisBlock(timestamp, author,coinbase, parentHash, null, 0,
                new BigInteger("0"), energonCeiling, difficultyBI, extraData);

        genesisBlock.getBlockHeader().setReceiptRoot(TrieImpl.EMPTY_TRIE_HASH);
        genesisBlock.getBlockHeader().setPermissionRoot(TrieImpl.EMPTY_TRIE_HASH);
        genesisBlock.getBlockHeader().setDposRoot(TrieImpl.EMPTY_TRIE_HASH);
        genesisBlock.getBlockHeader().setVotingRoot(TrieImpl.EMPTY_TRIE_HASH);
        genesisBlock.getBlockHeader().setTransactionRoot(TrieImpl.EMPTY_TRIE_HASH);
        genesisBlock.getBlockHeader().setTransferRoot(TrieImpl.EMPTY_TRIE_HASH);

        return genesisBlock;
    }

    private static GenesisJson parseGenesisJsonStr(String genesisJsonStr) {
        GenesisJson genesisJson = new GenesisJson();
        Map<String, String> genesisMap = JSON.parseObject(genesisJsonStr, new TypeReference<Map<String, String>>(){});
        genesisJson.setAutor(genesisMap.get("autor"));
        genesisJson.setCoinbase(genesisMap.get("coinbase"));
        genesisJson.setTimestamp(genesisMap.get("timestamp"));
        genesisJson.setEnergonCeiling(genesisMap.get("energonCeiling"));
        genesisJson.setDifficulty(genesisMap.get("difficulty"));
        genesisJson.setPerentHash(genesisMap.get("perentHash"));
        genesisJson.setExtraData(genesisMap.get("extraData"));
        genesisJson.setAccounts(JSON.parseObject(genesisMap.get("accounts"), new TypeReference<Map<String, GenesisJson.AccountJson>>() {
        }));
        genesisJson.setJsonBlockchainConfig(JSON.parseObject(genesisMap.get("config"), new TypeReference<GenesisConfig>() {
        }));
        return genesisJson;
    }

}

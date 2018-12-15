package org.platon.p2p.redir;

import com.google.protobuf.ByteString;
import org.platon.p2p.proto.common.ResourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yangzhou
 * @create 2018-04-28 11:16
 */

public class KeySelector {

    private static Logger logger = LoggerFactory.getLogger(KeySelector.class);



    private int branchingFactor;
    private int startLevel;
    private BigInteger lowestKey;
    private BigInteger highestKey;
    private String namespace;
    private int level;
    private KeyAlgorithm.KeyAlgorithmFunction keyAlgorithmFunction;


    public static class Builder {
        private int branchingFactor;
        private int startLevel;
        private BigInteger lowestKey;
        private BigInteger highestKey;
        private String namespace;
        private int level;
        private KeyAlgorithm.KeyAlgorithmFunction keyAlgorithmFunction;

        public Builder() {
        }

        public Builder branchingFactor(int branchingFactor){
           this.branchingFactor = branchingFactor;
           return this;
        }
        public Builder startLevel(int startLevel){
            this.startLevel = startLevel;
            return this;
        }

        public Builder lowestKey(BigInteger lowestKey){
            this.lowestKey = lowestKey;
            return this;
        }

        public Builder highestKey(BigInteger highestKey){
            this.highestKey = highestKey;
            return this;
        }

        public Builder namespace(String namespace){
            this.namespace = namespace;
            return this;
        }


        public Builder level(int level){
            this.level = level;
            return this;
        }

        public Builder keyAlgorithmFunction(KeyAlgorithm.KeyAlgorithmFunction keyAlgorithmFunction){
            this.keyAlgorithmFunction = keyAlgorithmFunction;
            return this;
        }
        KeySelector build(){
            return new KeySelector(this);
        }
    }


    private KeySelector(Builder builder){
        this.branchingFactor = builder.branchingFactor;
        this.startLevel = builder.startLevel;
        this.lowestKey = builder.lowestKey;
        this.highestKey = builder.highestKey;
        this.namespace = builder.namespace;
        this.level = builder.level;
        this.keyAlgorithmFunction = builder.keyAlgorithmFunction;
    }

    public int getBranchingFactor() {
        return branchingFactor;
    }

    public int getStartLevel() {
        return startLevel;
    }

    public BigInteger getLowestKey() {
        return lowestKey;
    }

    public BigInteger getHighestKey() {
        return highestKey;
    }

    public String getNamespace() {
        return namespace;
    }

    public KeyAlgorithm.KeyAlgorithmFunction getKeyAlgorithmFunction() {
        return keyAlgorithmFunction;
    }

    public int getLevel() {
        return level;
    }

    private ResourceID getResourceIDMock(byte[] key) {
        MessageDigest mdTemp = null;
        try {
            mdTemp = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            logger.error("error:", e);
        }
        mdTemp.update(key);
        return ResourceID.newBuilder().setId(ByteString.copyFrom(mdTemp.digest())).build();
    }

    public ResourceID getResourceID(Integer level, String key) {


        BigInteger value = keyAlgorithmFunction.apply(key, this);

        if (lowestKey.compareTo(value) > 0 || highestKey.compareTo(value) < 0) {
            logger.warn("get resource nodeId failed key:{} lowest key:{} highest key:{}", key.toString(), lowestKey.toString(), highestKey.toString());
            return null;
        }

        BigInteger pos = value.subtract(lowestKey);
        BigInteger keyspace = highestKey.subtract(lowestKey);


        BigInteger step = keyspace.divide(BigInteger.valueOf((long) Math.pow(branchingFactor, level-1) * 2));
        BigInteger node = pos.divide(step).add(pos.mod(step).compareTo(BigInteger.valueOf(0)) == 0 ? BigInteger.valueOf(0) : BigInteger.valueOf(1));


        MessageDigest mdTemp = null;
        try {
            mdTemp = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            logger.error("error:", e);
        }

        node = node.divide(BigInteger.valueOf(2)).add(node.mod(BigInteger.valueOf(2)).compareTo(BigInteger.valueOf(0)) == 0 ? BigInteger.valueOf(0) : BigInteger.valueOf(1));
        String nodeId = namespace + level.toString() + node.toString();

        mdTemp.update(nodeId.getBytes());
        return ResourceID.newBuilder().setId(ByteString.copyFrom(mdTemp.digest())).build();

    }

    public boolean isBoundary(Integer level, BigInteger key) {


        key = key.abs();
        if (lowestKey.compareTo(key) > 0 || highestKey.compareTo(key) < 0) {
            logger.warn("is boundary failed key:{} lowest key:{} highest key:{}", key.toString(), lowestKey.toString(), highestKey.toString());
            return false;
        }
        BigInteger pos = key.subtract(lowestKey);
        BigInteger keyspace = highestKey.subtract(lowestKey);


        BigInteger step = keyspace.divide(BigInteger.valueOf((long) Math.pow(branchingFactor, level-1) * 2));
        BigInteger node = pos.divide(step).add(pos.mod(step).compareTo(BigInteger.valueOf(0)) == 0 ? BigInteger.valueOf(0) : BigInteger.valueOf(1));




        if (key.compareTo(step.multiply(node.subtract(BigInteger.valueOf(1)))) == 0
                || key.compareTo(step.multiply(node).subtract(BigInteger.valueOf(1))) == 0 ){
            return true;
        }
        return false;
    }

    public BigInteger generateKey(String key) {
        return keyAlgorithmFunction.apply(key, this);
    }

    public List<ResourceID> getAllResourceID() {
        return getAllResourceID(startLevel);
    }
    public List<ResourceID> getAllResourceID(Integer level) {
        List<ResourceID> resourceIDS = new ArrayList<>();
        long levelSize = (long) Math.pow(branchingFactor, level-1);
        for (long i = 0; i < levelSize; i++) {
            MessageDigest mdTemp = null;
            try {
                mdTemp = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                logger.error("error:", e);
            }

            String nodeid = namespace + level.toString() + Long.toString(i);

            mdTemp.update(nodeid.getBytes());
            ResourceID r = ResourceID.newBuilder().setId(ByteString.copyFrom(mdTemp.digest())).build();
            resourceIDS.add(r);
        }
        return resourceIDS;
    }

}

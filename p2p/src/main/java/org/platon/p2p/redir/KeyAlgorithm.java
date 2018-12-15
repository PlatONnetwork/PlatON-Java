package org.platon.p2p.redir;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangzhou
 * @create 2018-07-23 11:04
 */
public class KeyAlgorithm {

    private static Map<String, KeyAlgorithmFunction> keyFunction = new ConcurrentHashMap<>();

    static {
        add("hashrate", new HashRate());
    }


    public static void add(String serviceType, KeyAlgorithmFunction func) {
        keyFunction.put(serviceType, func);
    }

    public static void remove(String serviceType) {
        keyFunction.remove(serviceType);
    }

    public static KeyAlgorithmFunction get(String serviceType) {
        return keyFunction.get(serviceType);
    }

    public interface KeyAlgorithmFunction {
        BigInteger apply(String key, KeySelector selector);
    }



    private static class HashRate implements KeyAlgorithmFunction{

        @Override
        public BigInteger apply(String key, KeySelector selector) {
            BigInteger num = new BigInteger(key);

            if (num.compareTo(selector.getLowestKey()) < 0 || num.compareTo(selector.getHighestKey()) > 0) {
                throw new RuntimeException("out range of key space");
            }

            return num;
        }
    }
}

package org.platon.p2p.redir;

import org.platon.p2p.common.ReDiRConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yangzhou
 * @create 2018-07-25 19:48
 */
public class KeySelectorFactory {

    private static Map<String, KeySelector> selectorMap = new HashMap<>();
    public static KeySelector get(String serviceType) {
        KeySelector selector = selectorMap.get(serviceType);
        if (selector != null) {
            return selector;
        }

        selector = new KeySelector.Builder().
                branchingFactor(ReDiRConfig.getInstance().getBranchingFactor(serviceType)).
                namespace(serviceType).
                level(ReDiRConfig.getInstance().getLevel(serviceType)).
                lowestKey(ReDiRConfig.getInstance().getLowestKey(serviceType)).
                highestKey(ReDiRConfig.getInstance().getHighestKey(serviceType)).
                startLevel(ReDiRConfig.getInstance().getStartLevel(serviceType)).
                keyAlgorithmFunction(KeyAlgorithm.get(ReDiRConfig.getInstance().getAlgorithm(serviceType))).build();
        return selector;
    }
}

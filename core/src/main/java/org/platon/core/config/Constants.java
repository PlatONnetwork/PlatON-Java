package org.platon.core.config;

/**
 * Describes different constants
 */
public class Constants {

    public static final int PERMISSION_TRIE_THRESHOLD = 1024;

    public static final String DATABASE_SOURCE_LEVELDB = "leveldb";
    public static final String DATABASE_SOURCE_ROCKSDB = "rocksdb";

    public static final int ERROR__1001_UNLOCK_ACCOUNT  = 1001;
    public static final int ERROR__1002_KEY_NOT_FOUND   = 1002;

    /**
     * New DELEGATECALL opcode introduced in the Homestead release. Before Homestead this opcode should generate
     * exception
     */
    public boolean hasDelegateCallOpcode() {return false; }

    /**
     * Introduced in the Homestead release
     */
    public boolean createEmptyContractOnOOG() {
        return true;
    }

    public int getMAX_CONTRACT_SZIE() { return Integer.MAX_VALUE; }
}

package org.platon.core;

import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.wrapper.DataWord;
import org.platon.core.block.Block;
import org.platon.core.db.ContractDetails;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author - Jungle
 * @date 2018/9/3 14:29
 * @version 0.0.1
 */
public interface Repository {

    boolean isContract(byte[] addr);

    Account createAccount(byte[] addr);

    BigInteger addBalance(byte[] addr, BigInteger value);

    BigInteger subBalance(byte[] addr, BigInteger value);

    BigInteger getBalance(byte[] addr);

    boolean isExist(byte[] addr);

    Account getAccount(byte[] addr);

    void delete(byte[] addr);

    ContractDetails getContractDetails(byte[] addr);

    boolean hasContractDetails(byte[] addr);

    void saveCode(byte[] addr, byte[] code);

    byte[] getCode(byte[] addr);

    byte[] getCodeHash(byte[] addr);

    void addStorageRow(byte[] addr, DataWord key, DataWord value);

    DataWord getStorageValue(byte[] addr, DataWord key);

    Set<byte[]> getAccountsKeys();

    void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash);

    Repository startTracking();

    void flush();

    void flushNoReconnect();

    void commit();

    void rollback();

    void syncToRoot(byte[] root);

    boolean isClosed();

    void close();

    void reset();

    void updateBatch(HashMap<ByteArrayWrapper, Account> accountStates,
                     HashMap<ByteArrayWrapper, ContractDetails> contractDetailes);

    byte[] getRoot();

    void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, Account> cacheAccounts,
                     HashMap<ByteArrayWrapper, ContractDetails> cacheDetails);

    Repository getSnapshotTo(byte[] root);

    /**
     * Retrieve storage size for a given account
     *
     * @param addr of the account
     * @return storage entries count
     */
    int getStorageSize(byte[] addr);

    /**
     * Retrieve all storage keys for a given account
     *
     * @param addr of the account
     * @return set of storage keys or empty set if account with specified address not exists
     */
    Set<DataWord> getStorageKeys(byte[] addr);

    /**
     * Retrieve storage entries from an account for given keys
     *
     * @param addr of the account
     * @param keys
     * @return storage entries for specified keys, or full storage if keys parameter is <code>null</code>
     */
    Map<DataWord, DataWord> getStorage(byte[] addr, @Nullable Collection<DataWord> keys);
}

package org.platon.core.db;

import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.wrapper.DataWord;
import org.platon.core.Account;
import org.platon.core.block.Block;
import org.platon.core.BlockchainImpl;
import org.platon.core.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class RepositoryWrapper implements Repository {

    @Autowired
    BlockchainImpl blockchain;

    public RepositoryWrapper() {
    }

    @Override
    public boolean isContract(byte[] addr) {
        return  blockchain.getRepository().isContract(addr);
    }

    @Override
    public Account createAccount(byte[] addr) {
        return blockchain.getRepository().createAccount(addr);
    }

    @Override
    public boolean isExist(byte[] addr) {
        return blockchain.getRepository().isExist(addr);
    }

    @Override
    public Account getAccount(byte[] addr) {
        return blockchain.getRepository().getAccount(addr);
    }

    @Override
    public void delete(byte[] addr) {
        blockchain.getRepository().delete(addr);
    }

    @Override
    public ContractDetails getContractDetails(byte[] addr) {
        return blockchain.getRepository().getContractDetails(addr);
    }

    @Override
    public boolean hasContractDetails(byte[] addr) {
        return blockchain.getRepository().hasContractDetails(addr);
    }

    @Override
    public void saveCode(byte[] addr, byte[] code) {
        blockchain.getRepository().saveCode(addr, code);
    }

    @Override
    public byte[] getCode(byte[] addr) {
        return blockchain.getRepository().getCode(addr);
    }

    @Override
    public byte[] getCodeHash(byte[] addr) {
        return blockchain.getRepository().getCodeHash(addr);
    }

    @Override
    public void addStorageRow(byte[] addr, DataWord key, DataWord value) {
        blockchain.getRepository().addStorageRow(addr, key, value);
    }

    @Override
    public DataWord getStorageValue(byte[] addr, DataWord key) {
        return blockchain.getRepository().getStorageValue(addr, key);
    }

    @Override
    public BigInteger getBalance(byte[] addr) {
        return blockchain.getRepository().getBalance(addr);
    }

    @Override
    public BigInteger addBalance(byte[] addr, BigInteger value) {
        return blockchain.getRepository().addBalance(addr, value);
    }

    @Override
    public BigInteger subBalance(byte[] addr, BigInteger value) {
        return blockchain.getRepository().subBalance(addr, value);
    }

    @Override
    public Set<byte[]> getAccountsKeys() {
        return blockchain.getRepository().getAccountsKeys();
    }

    @Override
    public void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash) {
        blockchain.getRepository().dumpState(block, gasUsed, txNumber, txHash);
    }

    @Override
    public Repository startTracking() {
        return blockchain.getRepository().startTracking();
    }

    @Override
    public void flush() {
        blockchain.getRepository().flush();
    }

    @Override
    public void flushNoReconnect() {
        blockchain.getRepository().flushNoReconnect();
    }

    @Override
    public void commit() {
        blockchain.getRepository().commit();
    }

    @Override
    public void rollback() {
        blockchain.getRepository().rollback();
    }

    @Override
    public void syncToRoot(byte[] root) {
        blockchain.getRepository().syncToRoot(root);
    }

    @Override
    public boolean isClosed() {
        return blockchain.getRepository().isClosed();
    }

    @Override
    public void close() {
        blockchain.getRepository().close();
    }

    @Override
    public void reset() {
        blockchain.getRepository().reset();
    }

    @Override
    public void updateBatch(HashMap<ByteArrayWrapper, Account> accountStates, HashMap<ByteArrayWrapper, ContractDetails> contractDetailes) {
        blockchain.getRepository().updateBatch(accountStates, contractDetailes);
    }

    @Override
    public byte[] getRoot() {
        return blockchain.getRepository().getRoot();
    }

    @Override
    public void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, Account> cacheAccounts, HashMap<ByteArrayWrapper, ContractDetails> cacheDetails) {
        blockchain.getRepository().loadAccount(addr, cacheAccounts, cacheDetails);
    }

    @Override
    public Repository getSnapshotTo(byte[] root) {
        return blockchain.getRepository().getSnapshotTo(root);
    }

    @Override
    public int getStorageSize(byte[] addr) {
        return blockchain.getRepository().getStorageSize(addr);
    }

    @Override
    public Set<DataWord> getStorageKeys(byte[] addr) {
        return blockchain.getRepository().getStorageKeys(addr);
    }

    @Override
    public Map<DataWord, DataWord> getStorage(byte[] addr, @Nullable Collection<DataWord> keys) {
        return blockchain.getRepository().getStorage(addr, keys);
    }

}

package org.platon.core;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.ByteComparator;
import org.platon.core.enums.AccountTypeEnum;
import org.platon.core.proto.AccountProto;
import org.platon.crypto.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Account
 *
 * @author yanze
 * @desc base account model
 * @create 2018-07-26 17:02
 **/
public class Account {

    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    private byte[] protoBuf;

    private BigInteger balance = BigInteger.ZERO;

    private byte[] storageRoot;

    private byte[] binHash;

    private byte[] bin;

    private byte[] permissionRoot;

    private boolean dirty;

    private Map<ByteArrayWrapper,byte[]> storageMap;

    public Account(byte[] protoBuf){
        this.protoBuf = protoBuf;
        //analyze account proto
        try {
            AccountProto accountProto = AccountProto.parseFrom(protoBuf);
            this.balance = new BigInteger(accountProto.getBalance().toByteArray());
            this.binHash = accountProto.getBinHash().toByteArray();
            this.storageRoot = accountProto.getExtraRoot().toByteArray();
            this.permissionRoot = accountProto.getPermissionRoot().toByteArray();
            this.dirty = false;
            this.storageMap = new LinkedHashMap<>();
        } catch (InvalidProtocolBufferException e) {
            this.protoBuf = null;
            logger.error("init account error",e);
        }
    }

    public Account(BigInteger balance, byte[] storageRoot, byte[] binHash, byte[] permissionRoot) {
        this.balance = balance;
        this.storageRoot = storageRoot;
        this.binHash = binHash;
        this.permissionRoot = permissionRoot;
        this.storageMap = new LinkedHashMap<>();
        this.dirty = balance.compareTo(BigInteger.ZERO) > 0;
    }

    public byte[] getEncoded(){
        if(dirty){
            //proto encode
            AccountProto.Builder accountProtoBuilder = AccountProto.newBuilder();
            accountProtoBuilder.setBalance(ByteString.copyFrom(this.balance.toByteArray()));
            accountProtoBuilder.setBinHash(ByteString.copyFrom(this.binHash));
            accountProtoBuilder.setExtraRoot(ByteString.copyFrom(this.storageRoot));
            accountProtoBuilder.setPermissionRoot(ByteString.copyFrom(this.permissionRoot));
            AccountProto accountProto = accountProtoBuilder.build();
            this.protoBuf = accountProto.toByteArray();
        }
        return protoBuf;
    }

    public boolean addBalance(BigInteger value){
        BigInteger result = balance.add(value);
        if (result.compareTo(BigInteger.ZERO) < 0) {
            //result balance < 0
            return false;
        }
        balance = result;
        this.dirty = true;
        return true;
    }

    public AccountTypeEnum getAccountType(){
        if(Arrays.equals(HashUtil.EMPTY_HASH,this.binHash)){
            return AccountTypeEnum.ACCOUNT_TYPE_EXTERNAL;
        }else{
            return AccountTypeEnum.ACCOUNT_TYPE_CONTRACT;
        }
    }

    public boolean isEmpty(){
        return Arrays.equals(HashUtil.EMPTY_HASH,storageRoot)
                &&Arrays.equals(HashUtil.EMPTY_HASH,binHash)
                &&Arrays.equals(HashUtil.EMPTY_HASH,permissionRoot)
                &&BigInteger.ZERO.equals(balance);
    }

    public void untouch(){
        this.dirty = false;
    }

    public boolean isContractExist() {
        return !ByteComparator.equals(binHash, HashUtil.EMPTY_HASH);
    }

    public byte[] getBin() {
        return bin;
    }

    public void setBin(byte[] bin) {
        this.binHash = HashUtil.sha3(bin);
        this.bin = bin;
    }

    public void setStorage(byte[] key, byte[] value){
        storageMap.put(new ByteArrayWrapper(key),value);
    }

    public byte[] getStorage(byte[] key){
        return storageMap.get(new ByteArrayWrapper(key));
    }

    public Map<ByteArrayWrapper, byte[]> getStorageMap() {
        return storageMap;
    }

    public void setPermissionRoot(byte[] permissionRoot) {
        this.permissionRoot = permissionRoot;
    }

    public BigInteger getBalance() {
        return balance;
    }

    public byte[] getStorageRoot() {
        return storageRoot;
    }

    public void setStorageRoot(byte[] storageRoot) {
        this.storageRoot = storageRoot;
    }

    public byte[] getBinHash() {
        return binHash;
    }

    public byte[] getPermissionRoot() {
        return permissionRoot;
    }

    public boolean isDirty() {
        return dirty;
    }

}


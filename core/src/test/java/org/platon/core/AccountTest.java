package org.platon.core;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class AccountTest {

    final BigInteger balance = new BigInteger("10000");
    final byte[] storageRoot = "storageRoot".getBytes();
    final byte[] binHash = "binHash".getBytes();
    final byte[] permissionRoot = "permissionRoot".getBytes();

    @Test
    public void getEncoded() {
        Account account = this.init();
        Account account1 = new Account(account.getEncoded());
        Assert.assertArrayEquals(account.getStorageRoot(),account1.getStorageRoot());
    }

    @Test
    public void addBalance() {
        Account account = this.init();
        BigInteger addBalance = new BigInteger("-1");
        account.addBalance(addBalance);
        Assert.assertEquals(account.getBalance().toString(),balance.add(addBalance).toString());
    }

    @Test
    public void isEmpty() {
        Account account = this.init();
        Assert.assertFalse(account.isEmpty());
    }

    @Test
    public void untouch() {
        Account account = this.init();
        account.untouch();
        Assert.assertFalse(account.isDirty());
    }

    private Account init(){
        return new Account(balance,storageRoot,binHash,permissionRoot);
    }
}
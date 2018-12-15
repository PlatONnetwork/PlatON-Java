package org.platon.core;

import java.math.BigInteger;

/**
 * ExternalAccount
 *
 * @author yanze
 * @desc external account for platON
 * @create 2018-07-26 17:26
 **/
public class ExternalAccount extends Account{

    /**
     * nickName for im
     */
    private byte[] nickName;

    /**
     * contribution for consensus
     */
    BigInteger contribution;

    public ExternalAccount(BigInteger balance, byte[] extraRoot, byte[] binHash, byte[] permissionRoot,byte[] nickName,BigInteger contribution) {
        super(balance, extraRoot, binHash, permissionRoot);
        this.nickName = nickName;
        this.contribution = contribution;
    }

    public ExternalAccount(byte[] protoBuf) {
        super(protoBuf);
    }

    @Override
    public byte[] getEncoded() {
        return super.getEncoded();
    }

    public byte[] getNickName() {
        return nickName;
    }

    public BigInteger getContribution() {
        return contribution;
    }
}

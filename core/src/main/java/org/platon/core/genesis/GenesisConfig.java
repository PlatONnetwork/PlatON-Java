package org.platon.core.genesis;

import org.platon.common.utils.Numeric;
import org.platon.common.wrapper.DataWord;
import org.platon.core.config.BlockchainConfig;
import org.platon.core.config.Constants;
import org.platon.core.vm.EnergonCost;
import org.platon.core.vm.OpCode;
import org.platon.core.vm.program.Program;

import java.math.BigInteger;

/**
 * GenesisConfig
 *
 * @author yanze
 * @desc block chian config by json
 * @create 2018-08-01 11:21
 **/
public class GenesisConfig implements BlockchainConfig {

    private String consensus;

    private String energonLimit;

    public GenesisConfig() {
    }

    public GenesisConfig(String consensus, String energonLimit) {
        this.consensus = consensus;
        this.energonLimit = energonLimit;
    }

    @Override
    public Constants getConstants() {
        return null;
    }

    @Override
    public String getConsensus() {
        return consensus;
    }

    @Override
    public BigInteger getEnergonLimit() {
        return Numeric.toBigInt(energonLimit);
    }

    @Override
    public int getBlockProducingInterval() {
        return 1000;
    }

    //TODO Energon
    @Override
    public EnergonCost getEnergonCost() {
        return null;
    }

    //TODO Energon
    @Override
    public DataWord getCallEnergon(OpCode op, DataWord requestedEnergon, DataWord availableEnergon) throws Program.OutOfEnergonException {
        return null;
    }

    //TODO Energon
    @Override
    public DataWord getCreateEnergon(DataWord availableEnergon) {
        return null;
    }

    public void setConsensus(String consensus) {
        this.consensus = consensus;
    }

    public void setEnergonLimit(String energonLimit) {
        this.energonLimit = energonLimit;
    }
}

package org.platon.core.config;

import org.platon.common.wrapper.DataWord;
import org.platon.core.vm.EnergonCost;
import org.platon.core.vm.OpCode;
import org.platon.core.vm.program.Program;

import java.math.BigInteger;

/**
 * BlockchainConfig
 *
 * @author yanze
 * @desc blockchian config of net and chain info
 * @create 2018-07-31 17:01
 **/
public interface BlockchainConfig {

    /**
     * Get blockchain constants
     */
    Constants getConstants();

    /**
     * TODO : getConsensus must rewrite so return maybe not String
     * @return
     */
    String getConsensus();

    /**
     * max energon on block
     * @return
     */
    BigInteger getEnergonLimit();

    /**
     * interval of producing block
     */
    int getBlockProducingInterval();

    /**
     * EVM operations costs
     */
    EnergonCost getEnergonCost();

    /**
     * Calculates available energon to be passed for callee
     * Since EIP150
     * @param op  Opcode
     * @param requestedEnergon amount of Energon requested by the program
     * @param availableEnergon available Energon
     * @throws Program.OutOfEnergonException If passed args doesn't conform to limitations
     */
    DataWord getCallEnergon(OpCode op, DataWord requestedEnergon, DataWord availableEnergon) throws Program.OutOfEnergonException;

    /**
     * Calculates available energon to be passed for contract constructor
     * Since EIP150
     */
    DataWord getCreateEnergon(DataWord availableEnergon);

}

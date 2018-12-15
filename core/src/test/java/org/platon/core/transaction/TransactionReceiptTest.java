/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.platon.core.transaction;


import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.platon.common.wrapper.DataWord;
import org.platon.crypto.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 05.12.2014
 */
public class TransactionReceiptTest {

    private static final Logger logger = LoggerFactory.getLogger("TransactionReceiptTest");
    
    @Test // rlp decode
    public void test_flow() {
        byte[] key = Hex.decode("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4");
        byte stateRoot[] = HashUtil.sha3(key);
        byte[] cumulativeEnergon = Hex.decode("50005050");
        Bloom bloomFilter = new Bloom();

        byte[] address = Hex.decode("f82804881bc16d674ec8000094cd2a3d9f938e13");
        List<DataWord> topics = new ArrayList<>();
        String data1 = "topics1";
        String data2 = "topics2";
        String data3 = "topics3";
        topics.add(DataWord.of(data1.getBytes()));
        topics.add(DataWord.of(data2.getBytes()));
        topics.add(DataWord.of(data3.getBytes()));
        byte[] data = Hex.decode("8aa0966265cc49fa1f10f0445f035258d116563931022a3570");
        LogInfo log1 = new LogInfo(address, topics, data);

        byte[] addressAnother = Hex.decode("f82804881bc16d674ec8000094cd2a3d9f938e13");
        List<DataWord> topicsAnother = new ArrayList<>();
        String another1 = "Another1";
        String another2 = "Another2";
        String another3 = "Another3";
        topics.add(DataWord.of(another1.getBytes()));
        topics.add(DataWord.of(another2.getBytes()));
        topics.add(DataWord.of(another3.getBytes()));
        byte[] dataAnother = Hex.decode("8aa0966265cc49fa1f10f0445f035258d116563931022a3570");
        LogInfo log2 = new LogInfo(addressAnother, topicsAnother, dataAnother);

        List<LogInfo> logInfoList = new ArrayList<>();
        logInfoList.add(log1);
        logInfoList.add(log2);
        byte[] energonUsed = Hex.decode("5050");
        byte[] executionResult = Hex.decode("01");

        TransactionReceipt txRep = new TransactionReceipt(stateRoot, cumulativeEnergon, bloomFilter, logInfoList);
        txRep.setEnergonUsed(energonUsed);
        txRep.setExecutionResult(executionResult);

        byte txRepEncode[] = txRep.getEncoded();

        TransactionReceipt txRepNew = new TransactionReceipt(txRepEncode);

        Assert.assertArrayEquals(stateRoot, txRepNew.getStateRoot());
        Assert.assertArrayEquals(cumulativeEnergon,txRepNew.getCumulativeEnergon());
        Assert.assertTrue(logInfoList.toString().compareTo(txRepNew.getLogInfoList().toString()) == 0);
    }

    @Test
    public void test_2() {
//        byte[] rlp = Hex.decode("f9012ea02d0cd041158c807326dae7cf5f044f3b9d4bd91a378cc55781b75455206e0c368339dc68b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c08252088080");
//
//        TransactionReceipt txReceipt = new TransactionReceipt(rlp);
//        txReceipt.setExecutionResult(new byte[0]);
//        byte[] encoded = txReceipt.getEncoded();
//        TransactionReceipt txReceipt1 = new TransactionReceipt(encoded);
//        System.out.println(txReceipt1);

    }
}

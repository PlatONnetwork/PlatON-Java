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
package org.platon.core.vm.program;

import com.google.protobuf.InvalidProtocolBufferException;
import org.platon.common.BasicPbCodec;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.ByteUtil;
import org.platon.core.vm.OpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Anton Nashatyrev on 06.02.2017.
 */
public class ProgramPrecompile {

    private static final Logger logger = LoggerFactory.getLogger(ProgramPrecompile.class);
    private static final int version = 1;

    private Set<Integer> jumpdest = new HashSet<>();

    public byte[] serialize() {
        byte[][] jdBytes = new byte[jumpdest.size() + 1][];
        int cnt = 0;
        jdBytes[cnt++] = BasicPbCodec.encodeInt(version);
        for (Integer dst : jumpdest) {
            jdBytes[cnt++] = BasicPbCodec.encodeInt(dst);
        }

        return BasicPbCodec.encodeBytesList(jdBytes);
    }

    public static ProgramPrecompile deserialize(byte[] stream) {
        List<ByteArrayWrapper> bytesList = null;
        try {
            bytesList = BasicPbCodec.decodeBytesList(stream);
            int ver = ByteUtil.byteArrayToInt(bytesList.get(0).getData());
            if (ver != version) return null;
            ProgramPrecompile ret = new ProgramPrecompile();
            for (int i = 1; i < bytesList.size(); i++) {
                ret.jumpdest.add(ByteUtil.byteArrayToInt(bytesList.get(i).getData()));
            }
            return ret;
        } catch (InvalidProtocolBufferException e) {
            logger.error("ProgramPrecompile deserialize error",e);
            return null;
        }
    }

    public static ProgramPrecompile compile(byte[] ops) {
        ProgramPrecompile ret = new ProgramPrecompile();
        for (int i = 0; i < ops.length; ++i) {

            OpCode op = OpCode.code(ops[i]);
            if (op == null) continue;

            if (op.equals(OpCode.JUMPDEST)) ret.jumpdest.add(i);

            if (op.asInt() >= OpCode.PUSH1.asInt() && op.asInt() <= OpCode.PUSH32.asInt()) {
                i += op.asInt() - OpCode.PUSH1.asInt() + 1;
            }
        }
        return ret;
    }

    public boolean hasJumpDest(int pc) {
        return jumpdest.contains(pc);
    }

    public static void main(String[] args) throws Exception {
        ProgramPrecompile pp = new ProgramPrecompile();
        pp.jumpdest.add(100);
        pp.jumpdest.add(200);
        byte[] bytes = pp.serialize();

        ProgramPrecompile pp1 = ProgramPrecompile.deserialize(bytes);
        System.out.println(pp1.jumpdest);
    }
}

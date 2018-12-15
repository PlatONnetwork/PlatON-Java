package org.platon.p2p.plugins.kademlia;

import com.google.protobuf.ByteString;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.proto.common.RoutableID;

import java.math.BigInteger;


public class KademliaHelp {

    public static int getDistance(RoutableID from, RoutableID to)
    {
        
        return getDistance(from.getId(), to.getId());
    }

    public static int getDistance(ByteString from, ByteString to)
    {
        
        assert(from.size() == to.size());
        int length = from.toByteArray().length*8;
        return length - getFirstSetBitIndex(xor(from, to)) - 1;
    }

    public static byte[] xor(RoutableID from, RoutableID to)
    {
        return xor(from.getId(), to.getId());
    }

    public static byte[] xor(ByteString from, ByteString to)
    {
        assert(from.toByteArray().length == to.toByteArray().length);
        int length = from.toByteArray().length*8;
        byte[] result = new byte[length / 8];
        byte[] nidBytes = to.toByteArray();

        for (int i = 0; i < length / 8; i++)
        {
            result[i] = (byte) (from.toByteArray()[i] ^ nidBytes[i]);
        }

        return result;
    }

    public static int getFirstSetBitIndex(byte[] keyBytes)
    {
        int prefixLength = 0;

        for (byte b : keyBytes)
        {
            if (b == 0)
            {
                prefixLength += 8;
            }
            else
            {
                
                int count = 0;
                for (int i = 7; i >= 0; i--)
                {
                    boolean a = (b & (1 << i)) == 0;
                    if (a)
                    {
                        count++;
                    }
                    else
                    {
                        break;
                    }
                }

                
                prefixLength += count;

                
                break;
            }
        }
        return prefixLength;
    }
    public static BigInteger getInt(RoutableID n) {
        return new BigInteger(1, n.getId().toByteArray());
    }

   public static BigInteger getInt(ByteString n) {
        return new BigInteger(1, n.toByteArray());
    }

    public static BigInteger getInt(Bytes n) {
        return new BigInteger(1, n.getKey());
    }

}

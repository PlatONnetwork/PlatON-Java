package org.platon.common.wrapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.platon.common.utils.ByteArrayWrapper;
import org.platon.common.utils.ByteComparator;
import org.platon.common.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import static org.platon.common.utils.ByteUtil.numberOfLeadingZeros;

public final class DataWord implements Comparable<DataWord> {

    
    public static final int MAX_POW = 256;
    public static final BigInteger _2_256 = BigInteger.valueOf(2).pow(MAX_POW);
    public static final BigInteger MAX_VALUE = _2_256.subtract(BigInteger.ONE);
    public static final DataWord ZERO = new DataWord(new byte[32]);
    public static final DataWord ONE = DataWord.of((byte) 1);

    public static final long MEM_SIZE = 32 + 16 + 16;

    private final byte[] data;

    
    private DataWord(byte[] data) {
        if (data == null || data.length != 32) throw new RuntimeException("Input byte array should have 32 bytes in it!");
        this.data = data;
    }

    public static DataWord of(byte[] data) {
        if (data == null || data.length == 0) {
            return DataWord.ZERO;
        }

        int leadingZeroBits = numberOfLeadingZeros(data);
        int valueBits = 8 * data.length - leadingZeroBits;
        if (valueBits <= 8) {
            if (data[data.length - 1] == 0) return DataWord.ZERO;
            if (data[data.length - 1] == 1) return DataWord.ONE;
        }

        if (data.length == 32)
            return new DataWord(Arrays.copyOf(data, data.length));
        else if (data.length <= 32) {
            byte[] bytes = new byte[32];
            System.arraycopy(data, 0, bytes, 32 - data.length, data.length);
            return new DataWord(bytes);
        } else {
            throw new RuntimeException(String.format("Data word can't exceed 32 bytes: 0x%s", Hex.toHexString(data)));
        }
    }

    public static DataWord of(ByteArrayWrapper wrappedData) {
        return of(wrappedData.getData());
    }

    @JsonCreator
    public static DataWord of(String data) {
        return of(Hex.decode(data));
    }

    public static DataWord of(byte num) {
        byte[] bb = new byte[32];
        bb[31] = num;
        return new DataWord(bb);

    }

    public static DataWord of(int num) {
        return of(ByteUtil.intToBytes(num));
    }

    public static DataWord of(long num) {
        return of(ByteUtil.longToBytes(num));
    }

    
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    
    private byte[] copyData() {
        return Arrays.copyOf(data, data.length);
    }

    public byte[] getNoLeadZeroesData() {
        return ByteUtil.stripLeadingZeroes(copyData());
    }

    public byte[] getLast20Bytes() {
        return Arrays.copyOfRange(data, 12, data.length);
    }

    public BigInteger value() {
        return new BigInteger(1, data);
    }

    
    public int intValue() {
        int intVal = 0;

        for (byte aData : data) {
            intVal = (intVal << 8) + (aData & 0xff);
        }

        return intVal;
    }

    
    public int intValueSafe() {
        int bytesOccupied = bytesOccupied();
        int intValue = intValue();
        if (bytesOccupied > 4 || intValue < 0) return Integer.MAX_VALUE;
        return intValue;
    }

    
    public long longValue() {

        long longVal = 0;
        for (byte aData : data) {
            longVal = (longVal << 8) + (aData & 0xff);
        }

        return longVal;
    }

    
    public long longValueSafe() {
        int bytesOccupied = bytesOccupied();
        long longValue = longValue();
        if (bytesOccupied > 8 || longValue < 0) return Long.MAX_VALUE;
        return longValue;
    }

    public BigInteger sValue() {
        return new BigInteger(data);
    }

    public String bigIntValue() {
        return new BigInteger(data).toString();
    }

    public boolean isZero() {
        if (this == ZERO) return true;
        return this.compareTo(ZERO) == 0;
    }




    public boolean isNegative() {
        int result = data[0] & 0x80;
        return result == 0x80;
    }

    public DataWord and(DataWord word) {
        byte[] newData = this.copyData();
        for (int i = 0; i < this.data.length; ++i) {
            newData[i] &= word.data[i];
        }
        return new DataWord(newData);
    }

    public DataWord or(DataWord word) {
        byte[] newData = this.copyData();
        for (int i = 0; i < this.data.length; ++i) {
            newData[i] |= word.data[i];
        }
        return new DataWord(newData);
    }

    public DataWord xor(DataWord word) {
        byte[] newData = this.copyData();
        for (int i = 0; i < this.data.length; ++i) {
            newData[i] ^= word.data[i];
        }
        return new DataWord(newData);
    }

    public DataWord negate() {
        if (this.isZero()) return ZERO;
        return bnot().add(DataWord.ONE);
    }

    public DataWord bnot() {
        if (this.isZero()) {
            return new DataWord(ByteUtil.copyToArray(MAX_VALUE));
        }
        return new DataWord(ByteUtil.copyToArray(MAX_VALUE.subtract(this.value())));
    }



    public DataWord add(DataWord word) {
        byte[] newData = new byte[32];
        for (int i = 31, overflow = 0; i >= 0; i--) {
            int v = (this.data[i] & 0xff) + (word.data[i] & 0xff) + overflow;
            newData[i] = (byte) v;
            overflow = v >>> 8;
        }
        return new DataWord(newData);
    }


    public DataWord add2(DataWord word) {
        BigInteger result = value().add(word.value());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }




    public DataWord mul(DataWord word) {
        BigInteger result = value().multiply(word.value());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }


    public DataWord div(DataWord word) {

        if (word.isZero()) {
            return ZERO;
        }

        BigInteger result = value().divide(word.value());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }


    public DataWord sDiv(DataWord word) {

        if (word.isZero()) {
            return ZERO;
        }

        BigInteger result = sValue().divide(word.sValue());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }


    public DataWord sub(DataWord word) {
        BigInteger result = value().subtract(word.value());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }


    public DataWord exp(DataWord word) {
        BigInteger newData = value().modPow(word.value(), _2_256);
        return new DataWord(ByteUtil.copyToArray(newData));
    }


    public DataWord mod(DataWord word) {

        if (word.isZero()) {
            return ZERO;
        }

        BigInteger result = value().mod(word.value());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }

    public DataWord sMod(DataWord word) {

        if (word.isZero()) {
            return ZERO;
        }

        BigInteger result = sValue().abs().mod(word.sValue().abs());
        result = (sValue().signum() == -1) ? result.negate() : result;

        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }

    public DataWord addmod(DataWord word1, DataWord word2) {
        if (word2.isZero()) {
            return ZERO;
        }

        BigInteger result = value().add(word1.value()).mod(word2.value());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }

    public DataWord mulmod(DataWord word1, DataWord word2) {

        if (this.isZero() || word1.isZero() || word2.isZero()) {
            return ZERO;
        }

        BigInteger result = value().multiply(word1.value()).mod(word2.value());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }

    
    public DataWord shiftLeft(DataWord arg) {
        if (arg.value().compareTo(BigInteger.valueOf(MAX_POW)) >= 0) {
            return DataWord.ZERO;
        }
        
        BigInteger result = value().shiftLeft(arg.intValueSafe());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }

    
    public DataWord shiftRight(DataWord arg) {
        if (arg.value().compareTo(BigInteger.valueOf(MAX_POW)) >= 0) {
            return DataWord.ZERO;
        }

        BigInteger result = value().shiftRight(arg.intValueSafe());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }

    
    public DataWord shiftRightSigned(DataWord arg) {
        if (arg.value().compareTo(BigInteger.valueOf(MAX_POW)) >= 0) {
            if (this.isNegative()) {
                return DataWord.ONE.negate();
            } else {
                return DataWord.ZERO;
            }
        }

        BigInteger result = sValue().shiftRight(arg.intValueSafe());
        return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
    }

    @JsonValue
    @Override
    public String toString() {
        return Hex.toHexString(data);
    }

    public String toPrefixString() {

        byte[] pref = getNoLeadZeroesData();
        if (pref.length == 0) return "";

        if (pref.length < 7)
            return Hex.toHexString(pref);

        return Hex.toHexString(pref).substring(0, 6);
    }

    public String shortHex() {
        String hexValue = Hex.toHexString(getNoLeadZeroesData()).toUpperCase();
        return "0x" + hexValue.replaceFirst("^0+(?!$)", "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataWord that = (DataWord) o;

        return Arrays.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public int compareTo(DataWord o) {
        if (o == null) return -1;
        int result = ByteComparator.compareTo(
                data, 0, data.length,
                o.data, 0, o.data.length);

        return (int) Math.signum(result);
    }

    public DataWord signExtend(byte k) {
        if (0 > k || k > 31)
            throw new IndexOutOfBoundsException();
        byte mask = this.sValue().testBit((k * 8) + 7) ? (byte) 0xff : 0;
        byte[] newData = this.copyData();
        for (int i = 31; i > k; i--) {
            newData[31 - i] = mask;
        }
        return new DataWord(newData);
    }

    public int bytesOccupied() {
        int firstNonZero = ByteUtil.firstNonZeroByte(data);
        if (firstNonZero == -1) return 0;
        return 31 - firstNonZero + 1;
    }

    public boolean isHex(String hex) {
        return Hex.toHexString(data).equals(hex);
    }

    public String asString() {
        return new String(getNoLeadZeroesData());
    }
}

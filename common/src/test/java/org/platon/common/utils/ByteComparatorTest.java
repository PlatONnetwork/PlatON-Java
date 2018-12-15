package org.platon.common.utils;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore
public class ByteComparatorTest {

    @Test
    public void test01(){
        byte[] arr01 = new byte[]{1, 2, 3, 4};
        byte[] arr02 = new byte[]{1, 2, 3, 4};
        boolean res = ByteComparator.equals(arr01, arr02);
        assertEquals(true, res);
    }

    @Test
    public void test02(){
        byte[] arr01 = new byte[]{1, 2, 3, 4};
        byte[] arr02 = new byte[]{1, 2, 3, 5};
        boolean res = ByteComparator.equals(arr01, arr02);
        assertEquals(false, res);
    }

}
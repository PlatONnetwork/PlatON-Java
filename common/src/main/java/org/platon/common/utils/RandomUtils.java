package org.platon.common.utils;

import java.util.Random;


public class RandomUtils {

	public static byte[] randomBytes(int length) {
		byte[] result = new byte[length];
		new Random().nextBytes(result);
		return result;
	}

	
	public static int randomInt(int maxInt) {
		int result = 0;
		while (result == 0){
			result = (int)(Math.random()*maxInt);
		}
		return result;
	}
}

package org.platon.crypto;

public class CheckUtil {

    public static void check(boolean test, String message) {
        if (!test) throw new IllegalArgumentException(message);
    }
}

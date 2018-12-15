package org.platon.common;

import org.slf4j.LoggerFactory;

public interface AppenderName {

    final static String APPENDER_CONSENSUS = "consensus";

    final static String APPENDER_KEY_STORE = "keystore";

    final static String APPENDER_PLATIN  = "plain";

    final static String APPENDER_RPC = "grpc";

    final static String APPENDER_WALLET = "wallet";

    final static String APPENDER_DB = "db";

    final static String APPENDER_STATE = "state";

    final static String APPENDER_PENDING = "pending";

    final static String APPENDER_MINE = "mine";

    static void showWarn(String message, String... messages) {

        LoggerFactory.getLogger(APPENDER_PLATIN).warn(message);
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";

        System.err.println(ANSI_RED);
        System.err.println("");
        System.err.println("        " + message);
        for (String msg : messages) {
            System.err.println("        " + msg);
        }
        System.err.println("");
        System.err.println(ANSI_RESET);
    }

    static void showErrorAndExit(String message, String... messages) {

        LoggerFactory.getLogger(APPENDER_PLATIN).error(message);


        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";

        System.err.println(ANSI_RED);
        System.err.println("");
        System.err.println("        " + message);
        for (String msg : messages) {
            System.err.println("        " + msg);
        }
        System.err.println("");
        System.err.println(ANSI_RESET);

        throw new RuntimeException(message);
    }

}

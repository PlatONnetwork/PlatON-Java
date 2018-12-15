package org.platon.core.facade;

import org.apache.commons.lang3.BooleanUtils;
import org.platon.common.AppenderName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CmdInterface {

    private CmdInterface() {
    }

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    public static void call(String[] args) {
        try {
            Map<String, Object> cliOptions = new HashMap<>();
            for (int i = 0; i < args.length; ++i) {
                String arg = args[i];
                processHelp(arg);

                if (i + 1 >= args.length)
                    continue;

                if (processDbDirectory(arg, args[i + 1], cliOptions))
                    continue;
                if (processDbReset(arg, args[i + 1], cliOptions))
                    continue;
            }

            if (cliOptions.size() > 0) {
                logger.info("Overriding config file with CLI options: {}", cliOptions);
            }

            //ConfigProperties.getDefault().overrideParams(cliOptions);

        } catch (Throwable e) {
            logger.error("Error parsing command line: [{}]", e.getMessage());
            System.exit(1);
        }
    }

    private static void processHelp(String arg) {
        if ("--help".equals(arg)) {
            printHelp();
            System.exit(1);
        }
    }

    private static boolean processDbDirectory(String arg, String db, Map<String, Object> cliOptions) {
        if (!"-db".equals(arg))
            return false;
        logger.info("DB directory set to [{}]", db);
        //cliOptions.put(ConfigProperties.PROPERTY_DB_DIR, db);
        return true;
    }

    private static boolean processDbReset(String arg, String reset, Map<String, Object> cliOptions) {
        if (!"-reset".equals(arg))
            return false;
        Boolean resetFlag = interpret(reset);
        if (resetFlag == null) {
            throw new Error(String.format("Can't interpret DB reset arguments: %s %s", arg, reset));
        }
        logger.info("Resetting db set to [{}]", resetFlag);
        //cliOptions.put(ConfigProperties.PROPERTY_DB_RESET, resetFlag.toString());

        return true;
    }

    private static Boolean interpret(String arg) {
        return BooleanUtils.toBooleanObject(arg);
    }

    private static void printHelp() {
        System.out.println("--help                -- this help message ");
        System.out.println("-reset <yes/no>       -- reset yes/no the all database ");
        System.out.println("-db <db>              -- to setup the path for the database directory ");
        System.out.println();
        System.out.println("e.g: cli -reset no -db db-1 ");
        System.out.println();
    }

}

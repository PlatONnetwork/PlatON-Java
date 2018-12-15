package org.platon.core;

import org.platon.common.AppenderName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.*;

/**
 *
 * @author - Jungle
 * @version 0.0.1
 * @date 2018/9/6 16:20
 */
@Component
public class EventDispatchWorker {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    private static EventDispatchWorker instance;

    private static final int[] queueSizeWarnLevels = new int[]{0, 10_000, 50_000, 100_000, 250_000, 500_000, 1_000_000, 10_000_000};

    // queue
    private final BlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<Runnable>();


    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, executorQueue, r -> new Thread(r, "-EDW-")
    );

    private long taskStart;
    private Runnable lastTask;
    private int lastQueueSizeWarnLevel = 0;
    private int counter;

    public static EventDispatchWorker getDefault() {
        if (instance == null) {
            instance = new EventDispatchWorker() {
                @Override
                public void invokeLater(Runnable r) {
                    r.run();
                }
            };
        }
        return instance;
    }

    public void invokeLater(final Runnable task) {

        if (executor.isShutdown()) {
            return;
        }
        if (counter++ % 1000 == 0) {
            logStatus();
        }
        executor.submit(() -> {
            try {
                lastTask = task;
                taskStart = System.nanoTime();
                task.run();
                long t = (System.nanoTime() - taskStart) / 1_000_000;
                taskStart = 0;
                if (t > 1000) {
                    logger.warn("EDW task executed in more than 1 sec: " + t + "ms, " +
                            "Executor queue size: " + executorQueue.size());
                }
            } catch (Exception e) {
                logger.error("EDW task exception", e);
            }
        });
    }


    private void logStatus() {

        int curLevel = getSizeWarnLevel(executorQueue.size());
        if (lastQueueSizeWarnLevel == curLevel) {
            return;
        }

        synchronized (this) {
            if (curLevel > lastQueueSizeWarnLevel) {
                long t = taskStart == 0 ? 0 : (System.nanoTime() - taskStart) / 1_000_000;
                String msg = "EDW size grown up to " + executorQueue.size() + " (last task executing for " + t + " ms: " + lastTask;
                if (curLevel < 3) {
                    logger.info(msg);
                } else {
                    logger.warn(msg);
                }
            } else if (curLevel < lastQueueSizeWarnLevel) {
                logger.info("EDW size shrunk down to " + executorQueue.size());
            }
            lastQueueSizeWarnLevel = curLevel;
        }
    }

    private static int getSizeWarnLevel(int size) {

        int idx = Arrays.binarySearch(queueSizeWarnLevels, size);
        return idx >= 0 ? idx : -(idx + 1) - 1;
    }

    public void shutdown() {
        executor.shutdownNow();
        try {

            executor.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("shutdown: executor interrupted: {}", e.getMessage());
        }
    }
}

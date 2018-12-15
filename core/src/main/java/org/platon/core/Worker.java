package org.platon.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

/**
 * worker thread common
 *
 * @author alliswell
 * @since 2018/08/22
 */
public abstract class Worker implements Runnable {

    /**
     * logger handle
     */
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    /**
     * the status of the thread
     * 0: Stopped
     * 1: Starting
     * 2: Started
     * 3: Stopping
     * 4: Killing
     */
    private AtomicInteger status = new AtomicInteger(0);
    private enum WorkerState {
        Stopped((int)0),
        Starting((int)1),
        Started((int)2),
        Stopping((int)3),
        Killing((int)4);

        private int value;
        WorkerState(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
    /**
     * idle time in milliseconds
     */
    private int idleInMills;

    protected void setIdleInMills(int idleInMills) {
        this.idleInMills = idleInMills;
    }

    /*
     * @Author alliswell
     * @Description do the business work accordingly
     * @Date 9:46 2018/8/23
     **/
    protected abstract void doWork();

    /*
     * @Author alliswell
     * @Description keep the thread run when status is Started
     * @Date 18:37 2018/8/22
     **/
    protected void keepWorking() {

        while (WorkerState.Started.getValue() == status.get()) {
            doWork();

            try {
                sleep(idleInMills);
            } catch (InterruptedException e) {
                logger.error("worker was interrupted!");
                e.printStackTrace();
                return;
            }
        }
    }

    @Override
    public void run() {

        status.set(WorkerState.Starting.getValue());
        while (WorkerState.Killing.getValue() != status.get()) {
            try {
                status.compareAndSet(WorkerState.Starting.getValue(), WorkerState.Started.getValue());

                beforeWorking();
                keepWorking();
                doneWorking();

                status.updateAndGet(x -> x = (x==WorkerState.Started.getValue() || x==WorkerState.Killing.getValue() )? x: WorkerState.Starting.getValue());
                if(status.get() == WorkerState.Stopped.getValue()) {
                    sleep(20);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        stopWorking();
    }

    /**
     * ready to start work
     */
    protected void beforeWorking() {

    }

    /**
     * run after keepWorking()
     */
    protected void doneWorking() {
        status.set(WorkerState.Stopping.getValue());
    }

    /**
     * stop worker thread
     */
    protected void stopWorking() {
        if(!status.compareAndSet(WorkerState.Stopping.getValue(), WorkerState.Stopped.getValue())) {
            try {
                sleep(20);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * if current thread is working
     */
    public boolean isWorking() {
        return status.get() == WorkerState.Started.getValue();
    }

    public void terminate() {
        status.set(WorkerState.Killing.getValue());
    }
}

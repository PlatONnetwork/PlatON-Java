package org.platon.slice.grpc;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class StreamObserverManager<T> implements StreamObserver<T> {

    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private T response ;
    private boolean endFlag = false;
    private Throwable e;

    @Override
    public void onNext(T value) {
        this.response = value;
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
        this.e = t;
        endFlag = true;
        signal();
    }

    @Override
    public void onCompleted() {
        endFlag = true;
        signal();
    }

    public void signal(){
        try {
            lock.lock();

            condition.signal();

        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    public T getResponse() throws Exception {
        lock.lock();
        try {
            if(!endFlag){

                condition.await();

            }
            if (e != null) {
                throw new Exception(e);
            }
            if (null != this.response) {
                return this.response;
            }
        }catch (Exception e){
            throw e;
        }finally {
            lock.unlock();
        }
        return null;
    }
}

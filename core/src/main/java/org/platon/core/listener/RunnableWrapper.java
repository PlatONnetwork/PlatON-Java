package org.platon.core.listener;

public abstract class RunnableWrapper implements Runnable {

    private PlatonListener listener;
    private String info;

    public RunnableWrapper(PlatonListener listener, String info) {
        this.listener = listener;
        this.info = info;
    }

    @Override
    public String toString() {
        return "RunnableWrapper: " + info + " [listener: " + listener.getClass() + "]";
    }
}
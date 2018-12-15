package org.platon.storage.datasource;

public abstract class AbstractChainedSource<Key, Value, SourceKey, SourceValue> implements Source<Key, Value> {

    private Source<SourceKey, SourceValue> source;
    protected boolean flushSource;

    
    protected AbstractChainedSource() {
    }

    public AbstractChainedSource(Source<SourceKey, SourceValue> source) {
        this.source = source;
    }

    
    protected void setSource(Source<SourceKey, SourceValue> src) {
        source = src;
    }

    public Source<SourceKey, SourceValue> getSource() {
        return source;
    }

    public void setFlushSource(boolean flushSource) {
        this.flushSource = flushSource;
    }

    
    @Override
    public synchronized boolean flush() {
        boolean ret = flushImpl();
        if (flushSource) {
            ret |= getSource().flush();
        }
        return ret;
    }

    
    protected abstract boolean flushImpl();
}

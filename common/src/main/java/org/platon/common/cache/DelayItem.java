package org.platon.common.cache;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**

 * @author lvxy
 * @version 0.0.1
 * @date 2018/8/28 13:46
 */
public class DelayItem<T> implements Delayed {
    
    /**
     * Base of nanosecond timings, to avoid wrapping
     */
    private static final long NANO_ORIGIN = System.nanoTime();

    /**
     * Returns nanosecond time offset by origin
     */
    final static long now() {
        return System.nanoTime() - NANO_ORIGIN;
    }

    /**
     * Sequence number to break scheduling ties, and in turn to guarantee FIFO order among tied
     * entries.
     */
    private static final AtomicLong sequencer = new AtomicLong(0);

    /**
     * Sequence number to break ties FIFO
     */
    private final long sequenceNumber;

    /**
     * The time the task is enabled to execute in nanoTime units
     */
    private final long time;

    private final T item;

    public DelayItem(T submit, long time, TimeUnit unit) {
        this.time = now() + TimeUnit.NANOSECONDS.convert(time, unit);
        this.item = submit;
        this.sequenceNumber = sequencer.getAndIncrement();
    }

    public T getItem() {
        return this.item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelayItem<?> delayItem = (DelayItem<?>) o;
        return Objects.equals(item, delayItem.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item);
    }

    public long getDelay(TimeUnit unit) {
        long d = unit.convert(time - now(), TimeUnit.NANOSECONDS);
        return d;
    }

    public int compareTo(Delayed other) {
        if (other == this) // compare zero ONLY if same object
            return 0;
        if (other instanceof DelayItem) {
            DelayItem x = (DelayItem) other;
            long diff = time - x.time;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else if (sequenceNumber < x.sequenceNumber)
                return -1;
            else
                return 1;
        }
        long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }
}

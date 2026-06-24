package dev.mccue.ding;

import java.util.concurrent.TimeUnit;

/// A handle to a running schedule.
public final class ActiveSchedule implements AutoCloseable {
    final InterruptableRunnable close;
    final Promise promise;

    ActiveSchedule(InterruptableRunnable close, Promise promise) {
        this.close = close;
        this.promise = promise;
    }

    @Override
    public void close() throws InterruptedException {
        close.run();
    }

    public void await() throws InterruptedException {
        promise.latch.await();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return promise.latch.await(timeout, unit);
    }
}

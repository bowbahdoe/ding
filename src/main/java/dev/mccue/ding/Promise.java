package dev.mccue.ding;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

final class Promise {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<CountDownLatch> reference = new AtomicReference<>(latch);

    boolean deliver() {
        if (latch.getCount() > 0 && reference.compareAndSet(latch, null)) {
            latch.countDown();
            return true;
        }
        else {
            return false;
        }
    }
}

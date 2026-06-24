package dev.mccue.ding;

import java.time.Instant;

/// A task that is run at a scheduled time.
@FunctionalInterface
public interface ScheduledTask {
    void run(Instant time) throws Exception;

    default void onFinish() {
    }

    default boolean handleError(Exception e) {
        Schedule.LOG.warn("Error running scheduled task", e);
        return !(e instanceof InterruptedException);
    }
}

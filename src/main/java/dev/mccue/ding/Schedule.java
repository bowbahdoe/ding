package dev.mccue.ding;

import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.*;

import io.vavr.collection.Stream;

public final class Schedule {
    static final Logger LOG = LoggerFactory.getLogger(Schedule.class);

    private final Seq<Instant> times;

    private Schedule(Seq<Instant> times) {
        this.times = times;
    }

    public static Schedule of(Seq<Instant> times) {
        return new Schedule(Objects.requireNonNull(times, "times"));
    }

    public static Schedule periodic(Instant start, Duration duration) {
        return new Schedule(periodicSeq(start, duration));
    }

    public static Schedule periodic(Instant start, Period period) {
        return new Schedule(periodicSeq(start, period));
    }

    public static Seq<Instant> periodicSeq(Instant start, Duration duration) {
        return Stream.iterate(start, t -> t.plus(duration));
    }

    public static Seq<Instant> periodicSeq(Instant start, Period period) {
        return Stream.iterate(start, t -> t.plus(period));
    }

    public Schedule withoutPastTimes() {
        return new Schedule(withoutPastTimes(times));
    }

    public Schedule withoutPastTimes(Instant now) {
        return new Schedule(withoutPastTimes(times, now));
    }

    public static Seq<Instant> withoutPastTimes(Seq<Instant> times) {
        return withoutPastTimes(times, Instant.now());
    }

    public static Seq<Instant> withoutPastTimes(Seq<Instant> times, Instant now) {
        return times.dropWhile(time -> time.isBefore(now));
    }


    private static void doClose(
            ScheduledExecutorService pool,
            Promise promise,
            ScheduledTask task
    ) {
        pool.shutdown();
        if (promise.deliver()) {
            task.onFinish();
        }
    }

    private static void scheduleLoop(
            InstantSource clock,
            Promise promise,
            ScheduledExecutorService pool,
            ScheduledTask task,
            Seq<Instant> times
    ) {
        if (times.isEmpty()) {
            doClose(pool, promise, task);
            return;
        }

        var time = times.head();
        Runnable runnable = () -> {
            boolean keepScheduling;
            try {
                task.run(time);
                keepScheduling = true;
            } catch (Exception e) {
                try {
                    keepScheduling = task.handleError(e);
                } catch (Exception e2) {
                    LOG.error("Error calling error handler, stopping schedule", e2);
                    keepScheduling = false;
                }
            }

            if (keepScheduling) {
                scheduleLoop(clock, promise, pool, task, times.tail());
            }
            else {
                doClose(pool, promise, task);
            }
        };

        if (time != null) {
            pool.schedule(runnable, ChronoUnit.MILLIS.between(clock.instant(), time), TimeUnit.MILLISECONDS);
        } else {
            doClose(pool, promise, task);
        }
    }

    private static ThreadFactory defaultThreadFactory() {
        return Thread.ofPlatform()
                .name("ding-")
                .factory();
    }
    public ActiveSchedule run(ScheduledTask task) {
        return run(task, defaultThreadFactory(), InstantSource.system());
    }

    public ActiveSchedule run(ScheduledTask task, ThreadFactory threadFactory) {
        return run(task, threadFactory, InstantSource.system());
    }

    public ActiveSchedule run(ScheduledTask task, InstantSource instantSource) {
        return run(task, defaultThreadFactory(), instantSource);
    }

    public ActiveSchedule run(ScheduledTask task, ThreadFactory threadFactory, InstantSource instantSource) {
        var promise = new Promise();
        var pool = Executors.newScheduledThreadPool(1, threadFactory);

        scheduleLoop(instantSource, promise, pool, task, times);

        return new ActiveSchedule(() -> {
            pool.shutdownNow();

            if (!pool.awaitTermination(1, TimeUnit.MINUTES)) {
                LOG.info("Failed to terminate schedule pool after 1 minute.");
            }

            doClose(pool, promise, task);
        }, promise);
    }

    public static ActiveSchedule run(
            Seq<Instant> times,
            ScheduledTask task
    ) {
        return Schedule.of(times).run(task);
    }

    public static ActiveSchedule run(
            Seq<Instant> times,
            ScheduledTask task,
            ThreadFactory threadFactory
    ) {
        return Schedule.of(times).run(task, threadFactory);
    }

    public static ActiveSchedule run(
            Seq<Instant> times,
            ScheduledTask task,
            InstantSource instantSource
    ) {
        return Schedule.of(times).run(task, instantSource);
    }

    public static ActiveSchedule run(
            Seq<Instant> times,
            ScheduledTask task,
            ThreadFactory threadFactory,
            InstantSource instantSource
    ) {
        return Schedule.of(times).run(task, threadFactory, instantSource);
    }

    public static Stream<Instant> merge(
            Seq<Instant> left,
            Seq<Instant> right
    ) {
        var leftEmpty = left.isEmpty();
        var rightEmpty = right.isEmpty();
        if (leftEmpty && rightEmpty) {
            return Stream.empty();
        }
        else if (leftEmpty) {
            return right.toStream();
        }
        else if (rightEmpty) {
            return left.toStream();
        }
        else {
            var lHead = left.head();
            var rHead = right.head();

            if (lHead.isBefore(rHead)) {
                return Stream.cons(lHead, () -> merge(left.tail().toStream(), right.toStream()));
            }
            else {
                return Stream.cons(rHead, () -> merge(left.toStream(), right.tail().toStream()));
            }
        }
    }

    public Schedule merge(Schedule right) {
        return new Schedule(merge(this.times, right.times));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Schedule other && times.equals(other.times);
    }

    @Override
    public int hashCode() {
        return times.hashCode();
    }

    @Override
    public String toString() {
        return this.times.toString();
    }
}

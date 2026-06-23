import dev.mccue.ding.Schedule;
import dev.mccue.ding.ScheduledTask;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DingTest {

    private void checkTimeliness(Seq<Tuple2<Instant, Instant>> proof) {
        for (var timePair : proof) {
            assertEquals(timePair._1.toEpochMilli(), timePair._2.toEpochMilli(), 20);
        }
    }

    @Test
    public void scheduleTest() throws Exception {
        var now = Instant.now();
        var times = Vector.of(
                now.minusSeconds(2),
                now.plusSeconds(1),
                now.plusSeconds(2)
        );

        var proof = new AtomicReference<Vector<Tuple2<Instant, Instant>>>(Vector.empty());

        try (var sched = Schedule.of(times).run(t -> {
            proof.updateAndGet(v -> v.append(Tuple.of(t, Instant.now())));
        })) {
            Thread.sleep(2500);
        }

        assertEquals(times, proof.get().map(Tuple2::_1));
        checkTimeliness(proof.get().tail());
    }

    @Test
    public void emptyTimes() throws Exception {
        var proof = new AtomicBoolean(false);
        Schedule.of(Vector.empty())
                .run(new ScheduledTask() {
                    @Override
                    public void run(Instant time) {

                    }

                    @Override
                    public void onFinish() {
                        proof.set(true);
                    }
                })
                .await();

        assertTrue(proof.get());
    }

    @Test
    public void onFinished() throws Exception {
        var now = Instant.now();
        var proof = new AtomicBoolean(false);
        Schedule.of(Vector.of(now.plusMillis(500), now.plusMillis(1000)))
                .run(new ScheduledTask() {
                    @Override
                    public void run(Instant time) {

                    }

                    @Override
                    public void onFinish() {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        proof.set(true);
                    }
                });

        Thread.sleep(1200);

        assertTrue(proof.get());
    }
}

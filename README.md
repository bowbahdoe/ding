# DING!

Lightweight API for scheduling tasks based on [chime](https://github.com/jarohen/chime).

The idea here is the same as the original library.

A scheduler's job is to run a function at a (possibly infinite) sequence of times. You are free to generate this sequence
in whatever way you see fit. There are examples of a few ways below.

```xml
<dependency>
    <groupId>dev.mccue</groupId>
    <artifactId>ding</artifactId>
    <version>2026.06.23</version>
</dependency>
```

## Run a task every 5 minutes

```java
import dev.mccue.ding.Schedule;

void main() {
    Schedule.periodic(Instant.now(), t -> t.plus(Duration.ofMinutes(5)))
            .run(t -> IO.println("Hello: " + t));
}
```


## Run a task set number of times

```java
import dev.mccue.ding.Schedule;
import io.vavr.collection.Vector;

void main() {
    Schedule.of(Vector.of(
                Instant.now(),
                Instant.now().plus(Duration.ofHours(1))
            ))
            .run(t -> IO.println("Hello: " + t));
}
```

## Cancel a running schedule

```java
import dev.mccue.ding.Schedule;

void main() throws Exception {
    var activeSchedule = Schedule.periodic(Instant.now(), t -> t.plus(Duration.ofMinutes(1)))
            .run(t -> IO.println("Hello: " + t));
    
    IO.readln("Press Enter to Stop: ");
    
    activeSchedule.close();
}
```

## Abort Schedule depending on exception type

```java
import dev.mccue.ding.ScheduledTask;
import dev.mccue.ding.Schedule;

import java.io.IOException;
import java.time.Instant;

class Task implements ScheduledTask {
    @Override
    public void run(Instant time) throws Exception {
        if (Math.random() < 0.2) {
            throw new IOException();
        }
    }

    @Override
    public boolean handleError(Exception e) {
        if (e instanceof IOException) {
            IO.println("Do not continue scheduling after an IOException");
            return false;
        }
        else {
            IO.println("Falling back to default behavior (continue unless InterruptedException)");
            return ScheduledTask.super.handleError(e);
        }
    }
}


void main() {
    Schedule.periodic(Instant.now(), t -> t.plus(Duration.ofMinutes(5)))
            .run(new Task());
}
```

## Run a task based on a cron expression

```xml
<dependency>
    <groupId>com.cronutils</groupId>
    <artifactId>cron-utils</artifactId>
    <version>9.2.1</version>
</dependency>
```

```java
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import dev.mccue.ding.Schedule;
import io.vavr.collection.Stream;

void main() {
    var cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    var cronParser = new CronParser(cronDefinition);

    // "At 3 am on Friday."
    var cron = cronParser.parse("0 3 * * 5");

    var times = Stream.iterate(
                    ZonedDateTime.now(),
                    t -> ExecutionTime.forCron(cron).nextExecution(t).orElse(null)
            )
            .takeWhile(Objects::nonNull)
            .map(ZonedDateTime::toInstant);

    Schedule.of(times).withoutPastTimes()
            .run(time -> IO.println("Oh boy, 3am!"));
}
```

## Run a task every sunrise in New York City

```xml
<dependency>     
    <groupId>net.e175.klaus</groupId>
    <artifactId>solarpositioning</artifactId>
    <version>2.0.12</version>
</dependency>
```

```java
import dev.mccue.ding.Schedule;
import io.vavr.collection.Stream;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SunriseResult;

void main() {
    var sunrises = Stream.iterate(ZonedDateTime.now(), dt -> dt.plus(Period.ofDays(1)))
            .map(dt -> SPA.calculateSunriseTransitSet(
                    dt,
                    40.7128,
                    74.0060,
                    DeltaT.estimate(dt.toLocalDate())
            ))
            .flatMap(sunriseResult -> {
                if (sunriseResult instanceof SunriseResult.RegularDay regularDay) {
                    return Stream.of(regularDay.sunrise());
                } else {
                    return Stream.empty();
                }
            })
            .map(ZonedDateTime::toInstant);

    Schedule.of(sunrises)
            .withoutPastTimes()
            .run(time -> IO.println("Good Morning New York!"));
}
```
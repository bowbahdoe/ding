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

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
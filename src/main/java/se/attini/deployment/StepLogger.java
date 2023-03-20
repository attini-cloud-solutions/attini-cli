package se.attini.deployment;

import java.time.Instant;
import java.util.List;

public interface StepLogger {

    List<Line> lines();

    record Line(Instant timestamp, String data){}


}

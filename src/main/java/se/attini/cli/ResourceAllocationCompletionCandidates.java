package se.attini.cli;

import java.util.Iterator;
import java.util.List;

public class ResourceAllocationCompletionCandidates implements Iterable<String> {

    @Override
    public Iterator<String> iterator() {
        return List.of("Dynamic",
                       "Small",
                       "Medium",
                       "Large")
                   .iterator();
    }
}

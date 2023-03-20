package se.attini.inittemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Steps {
    private final Map<String, List<Step>> steps;

    Steps(Map<String, List<Step>> steps) {
        this.steps = steps;
    }


    public List<Step> get(String type){
        return steps.getOrDefault(type, Collections.emptyList());
    }
}


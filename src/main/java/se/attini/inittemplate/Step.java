package se.attini.inittemplate;

import com.fasterxml.jackson.databind.JsonNode;

public record Step(String name,
                   JsonNode definition) {

}

package se.attini.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RegionTest {

    @Test
    void shouldCreateOk() {
        Region.create("eu-west-1");
        Region.create("us-gov-west-1");
    }

    @Test
    void shouldFailOnNoneNumericEnd() {
       assertThrows(DomainConversionError.class, () ->  Region.create("eu-west-p"));
    }

    @Test
    void shouldFailOnToFewParts() {
        assertThrows(DomainConversionError.class, () ->  Region.create("eu-west"));
    }
}

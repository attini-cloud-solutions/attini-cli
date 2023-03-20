package se.attini.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void pad_same_length() {
        String value = "my-test-string";
        String result = StringUtils.pad(value, value.length());
        assertEquals(value, result);
    }

    @Test
    void pad_plus_10() {
        String value = "my-test-string";
        String result = StringUtils.pad(value, value.length() + 10);
        assertEquals( " ".repeat(10) + value, result);
    }

    @Test
    void pad_minus_10() {
        String value = "my-test-string";
        String result = StringUtils.pad(value, value.length() - 10);
        assertEquals( value, result);
    }

    @Test
    void pad_minus_100() {
        String value = "my-test-string";
        String result = StringUtils.pad(value, value.length() - 100);
        assertEquals(value, result);
    }

    @Test
    void cut_same_length() {
        String value = "my-test-string";
        String result = StringUtils.cut(value, value.length());
        assertEquals(value, result);

    }

    @Test
    void cut_minus_1() {
        String value = "my-test-string";
        String result = StringUtils.cut(value, value.length() -1);
        assertEquals("my-test-st...", result);
        assertEquals(value.length() - 1, result.length());

    }

    @Test
    void cut_below_min_value() {
        String value = "my-test-string";
        String result = StringUtils.cut(value, 2);
        assertEquals("my...", result);
        assertEquals(5, result.length());

    }


    @Test
    void cut_plus_10() {
        String value = "my-test-string";
        String result = StringUtils.cut(value, value.length() + 10);
        assertEquals(value, result);
    }
}

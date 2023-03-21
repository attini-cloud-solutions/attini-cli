package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class Region {

    private final String name;

    private Region(String region) {
        validate(requireNonNull(region, "name"));
        this.name = region;
    }

    private void validate(String region){
        String[] split = region.split("-");
        if (split.length < 3){
            throw new DomainConversionError("Illegal format for region: " + region);
        }
        try {
            Integer.valueOf(split[split.length - 1]);
        }catch (NumberFormatException e){
            throw new DomainConversionError("Illegal format for region: " + region);
        }

    }

    public String getName() {
        return name;
    }

    public static Region create(String name){
        return new Region(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Region region1 = (Region) o;
        return Objects.equals(name, region1.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Region{" +
               "region='" + name + '\'' +
               '}';
    }
}

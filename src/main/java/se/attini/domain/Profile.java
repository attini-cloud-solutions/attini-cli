package se.attini.domain;

import static java.util.Objects.requireNonNull;

public class Profile {

    private final String profile;

    private Profile(String profile) {
        this.profile = requireNonNull(profile, "profile");
    }

    public String getProfileName(){
        return this.profile;
    }

    public static Profile create(String profileName){
        return new Profile(profileName);
    }
}

package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class Email {

    private final String email;

    private Email(String email) {
        this.email = requireNonNull(email, "email");
    }

    public String getEmail() {
        return email;
    }

    public boolean isValid(){
        return email.matches("^(.+)@(.+)$");
    }

    public static Email create(String email){
        return new Email(email);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email1 = (Email) o;
        return Objects.equals(email, email1.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "Email{" +
               "email='" + email + '\'' +
               '}';
    }
}

package com.srm.creditengine.auth.domain;

public interface PasswordHasher {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}

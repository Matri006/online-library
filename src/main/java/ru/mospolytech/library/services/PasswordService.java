package ru.mospolytech.library.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ru.mospolytech.library.exceptions.LibraryException;

import java.nio.charset.StandardCharsets;

@Service
public class PasswordService {
    private final PasswordEncoder encoder;

    public PasswordService(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    public String encode(String password) {
        int bytes = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 8 || bytes > 72) {
            throw new LibraryException("INVALID_PASSWORD");
        }
        return encoder.encode(password);
    }
}

package ru.mospolytech.library.exceptions;

public class LibraryException extends RuntimeException {
    public final String code;

    public LibraryException(String code) {
        super(code);
        this.code = code;
    }
}

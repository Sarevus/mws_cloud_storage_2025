package com.MWS.exception;

public class DuplicateFileException extends RuntimeException {

    public DuplicateFileException(String message) {
        super("Folder with the same name already exists: " + message);
    }
}

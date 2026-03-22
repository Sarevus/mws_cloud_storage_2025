package com.MWS.exception;

public class DuplicateFolderException extends RuntimeException{

    public DuplicateFolderException(String message) {
        super("Folder with the same name already exists: " + message);
    }
}

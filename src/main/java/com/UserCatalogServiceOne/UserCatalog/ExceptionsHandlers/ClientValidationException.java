package com.UserCatalogServiceOne.UserCatalog.ExceptionsHandlers;

public class ClientValidationException extends RuntimeException {
    public ClientValidationException(String message) {
        super(message);
    }
}
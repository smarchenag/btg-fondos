package com.btg.fondos.exception;

public class AlreadySubscribedException extends RuntimeException {
    public AlreadySubscribedException(String fundName) {
        super("Ya se encuentra suscrito al fondo " + fundName);
    }
}

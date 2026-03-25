package com.btg.fondos.exception;

public class NotSubscribedException extends RuntimeException {
    public NotSubscribedException(String fundName) {
        super("No se encuentra suscrito al fondo " + fundName);
    }
}

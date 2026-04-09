package com.btg.fondos.exception;

public class FundNotFoundException extends RuntimeException {
    public FundNotFoundException(String fundId) {
        super("Fondo no encontrado con ID: " + fundId);
    }
}

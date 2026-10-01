package com.example.multas.domain.port;

public class PagoRechazadoException extends RuntimeException {
    public PagoRechazadoException(String mensaje) {
        super(mensaje);
    }
}
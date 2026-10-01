package com.example.multas.domain.port;

public record ResultadoPago(
        String proveedor,
        boolean exitoso,
        String referenciaExterna,
        String mensaje
) {}

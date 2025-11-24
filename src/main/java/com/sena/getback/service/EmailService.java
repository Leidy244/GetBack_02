package com.sena.getback.service;

public interface EmailService {
    void enviarCorreoRecuperacion(String correo, String token);
    void enviarCorreoSimple(String para, String asunto, String contenido);
}

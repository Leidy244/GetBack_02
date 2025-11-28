package com.sena.getback.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${server.port:57675}")
    private String serverPort;
 
    @Override
    public void enviarCorreoRecuperacion(String correo, String token) {

        String asunto = "Recuperación de Contraseña - GetBack";
        String url ="http://localhost:"+serverPort.trim()+"/reset-password?correo="+correo+"&token="+token;
        System.out.println("URL generada: " + url);
        System.out.println("Longitud URL: " + url.length());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(correo);
            helper.setSubject(asunto);
            String htmlContenido =
                    "<p>Hola.</p>" +
                    "<p>Has solicitado restablecer tu contraseña en GetBack.</p>" +
                    "<p>Puedes restablecer tu contraseña haciendo clic en el siguiente enlace:</p>" +
                    "<p><a href='"+url+"'>Restablecer contraseña</a></p>" +
                    "<p>Si no solicitaste este cambio, puedes ignorar este correo.</p>" +
                    "<p>Saludos.<br>El equipo de GetBack</p>";

            helper.setText(htmlContenido, true);

            mailSender.send(mimeMessage);
            System.out.println("Correo HTML enviado exitosamente a: " + correo);
        } catch (Exception e) {
            System.err.println("Error al enviar correo HTML: " + e.getMessage());
            

        }
    }

    @Override
    public void enviarCorreoSimple(String para, String asunto, String contenido) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(contenido);

            mailSender.send(mensaje);
            System.out.println("Correo enviado exitosamente a: " + para);
        } catch (Exception e) {
            System.err.println("Error al enviar correo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

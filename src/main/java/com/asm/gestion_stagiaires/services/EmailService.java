package com.asm.gestion_stagiaires.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void envoyerIdentifiants(String destinataire, String nom, String prenom,
                                    String email, String motDePasse, String role) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinataire);
        message.setSubject("ASM - Vos identifiants d'accès à la plateforme");
        message.setText(
                "Bonjour " + prenom + " " + nom + ",\n\n"
                        + "Votre demande d'accès en tant que " + role + " a été validée.\n\n"
                        + "Voici vos identifiants de connexion :\n"
                        + "Email : " + email + "\n"
                        + "Mot de passe : " + motDePasse + "\n\n"
                        + "Veuillez changer votre mot de passe après votre première connexion.\n\n"
                        + "Lien de la plateforme : http://localhost:8085\n\n"
                        + "Cordialement,\n"
                        + "L'équipe ASM"
        );
        mailSender.send(message);
    }
}
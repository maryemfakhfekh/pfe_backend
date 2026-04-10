package com.asm.gestion_stagiaires.Controller;

import com.asm.gestion_stagiaires.models.*;
import com.asm.gestion_stagiaires.repositories.DemandeAccesRepository;
import com.asm.gestion_stagiaires.repositories.UtilisateurRepository;
import com.asm.gestion_stagiaires.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private DemandeAccesRepository demandeAccesRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    // Voir les demandes en attente
    @GetMapping("/demandes-acces")
    public ResponseEntity<List<DemandeAcces>> getDemandesEnAttente() {
        return ResponseEntity.ok(demandeAccesRepository.findByStatut(StatutDemande.EN_ATTENTE));
    }

    // Voir toutes les demandes
    @GetMapping("/demandes-acces/toutes")
    public ResponseEntity<List<DemandeAcces>> getToutesDemandes() {
        return ResponseEntity.ok(demandeAccesRepository.findAll());
    }

    // Valider une demande
    @PutMapping("/demandes-acces/{id}/valider")
    public ResponseEntity<?> validerDemande(@PathVariable Long id) {
        DemandeAcces demande = demandeAccesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cette demande a déjà été traitée"));
        }

        if (utilisateurRepository.existsByEmail(demande.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Un compte avec cet email existe déjà"));
        }

        // Générer un mot de passe aléatoire
        String motDePasse = UUID.randomUUID().toString().substring(0, 8);

        // Créer le compte selon le rôle
        if ("RH".equalsIgnoreCase(demande.getRoleSouhaite())) {
            RH rh = new RH();
            rh.setNom(demande.getNom());
            rh.setPrenom(demande.getPrenom());
            rh.setEmail(demande.getEmail());
            rh.setPassword(passwordEncoder.encode(motDePasse));
            rh.setTelephone(demande.getTelephone());
            utilisateurRepository.save(rh);
        } else if ("ENCADRANT".equalsIgnoreCase(demande.getRoleSouhaite())) {
            Encadrant encadrant = new Encadrant();
            encadrant.setNom(demande.getNom());
            encadrant.setPrenom(demande.getPrenom());
            encadrant.setEmail(demande.getEmail());
            encadrant.setPassword(passwordEncoder.encode(motDePasse));
            encadrant.setTelephone(demande.getTelephone());
            encadrant.setDepartement(demande.getDepartement());
            encadrant.setSpecialite(demande.getSpecialite());
            utilisateurRepository.save(encadrant);
        }

        // Envoyer l'email avec les identifiants
        emailService.envoyerIdentifiants(
                demande.getEmail(),
                demande.getNom(),
                demande.getPrenom(),
                demande.getEmail(),
                motDePasse,
                demande.getRoleSouhaite()
        );

        // Mettre à jour le statut de la demande
        demande.setStatut(StatutDemande.VALIDEE);
        demandeAccesRepository.save(demande);

        return ResponseEntity.ok(Map.of("message", "Demande validée, compte créé et email envoyé"));
    }

    // Refuser une demande
    @PutMapping("/demandes-acces/{id}/refuser")
    public ResponseEntity<?> refuserDemande(@PathVariable Long id) {
        DemandeAcces demande = demandeAccesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cette demande a déjà été traitée"));
        }

        demande.setStatut(StatutDemande.REFUSEE);
        demandeAccesRepository.save(demande);

        return ResponseEntity.ok(Map.of("message", "Demande refusée"));
    }

    // Voir tous les utilisateurs
    @GetMapping("/utilisateurs")
    public ResponseEntity<List<Utilisateur>> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }

    // Supprimer un utilisateur
    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<?> supprimerUtilisateur(@PathVariable Long id) {
        utilisateurRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }
}
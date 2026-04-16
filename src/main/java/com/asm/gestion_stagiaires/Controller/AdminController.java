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

    // ===== CRÉATION DIRECTE =====

    @PostMapping("/creer-rh")
    public ResponseEntity<?> creerRH(@RequestBody Map<String, String> data) {
        if (utilisateurRepository.existsByEmail(data.get("email"))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Un compte avec cet email existe déjà"));
        }

        String motDePasse = UUID.randomUUID().toString().substring(0, 8);

        RH rh = new RH();
        rh.setNom(data.get("nom"));
        rh.setPrenom(data.get("prenom"));
        rh.setEmail(data.get("email"));
        rh.setPassword(passwordEncoder.encode(motDePasse));
        rh.setTelephone(data.get("telephone"));
        utilisateurRepository.save(rh);

        emailService.envoyerIdentifiants(
                data.get("email"),
                data.get("nom"),
                data.get("prenom"),
                data.get("email"),
                motDePasse,
                "RH"
        );

        return ResponseEntity.ok(Map.of("message", "Compte RH créé et email envoyé"));
    }

    @PostMapping("/creer-encadrant")
    public ResponseEntity<?> creerEncadrant(@RequestBody Map<String, String> data) {
        if (utilisateurRepository.existsByEmail(data.get("email"))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Un compte avec cet email existe déjà"));
        }

        String motDePasse = UUID.randomUUID().toString().substring(0, 8);

        Encadrant encadrant = new Encadrant();
        encadrant.setNom(data.get("nom"));
        encadrant.setPrenom(data.get("prenom"));
        encadrant.setEmail(data.get("email"));
        encadrant.setPassword(passwordEncoder.encode(motDePasse));
        encadrant.setTelephone(data.get("telephone"));
        encadrant.setDepartement(data.get("departement"));
        encadrant.setSpecialite(data.get("specialite"));
        utilisateurRepository.save(encadrant);

        emailService.envoyerIdentifiants(
                data.get("email"),
                data.get("nom"),
                data.get("prenom"),
                data.get("email"),
                motDePasse,
                "ENCADRANT"
        );

        return ResponseEntity.ok(Map.of("message", "Compte Encadrant créé et email envoyé"));
    }

    // ===== MODIFIER UN UTILISATEUR =====

    @PutMapping("/utilisateurs/{id}")
    public ResponseEntity<?> modifierUtilisateur(@PathVariable Long id, @RequestBody Map<String, String> data) {
        return utilisateurRepository.findById(id).map(user -> {
            if (data.containsKey("nom")) user.setNom(data.get("nom"));
            if (data.containsKey("prenom")) user.setPrenom(data.get("prenom"));
            if (data.containsKey("email")) user.setEmail(data.get("email"));
            if (data.containsKey("telephone")) user.setTelephone(data.get("telephone"));

            if (user instanceof Encadrant encadrant) {
                if (data.containsKey("departement")) encadrant.setDepartement(data.get("departement"));
                if (data.containsKey("specialite")) encadrant.setSpecialite(data.get("specialite"));
            }

            if (user instanceof Stagiaire stagiaire) {
                if (data.containsKey("etablissement")) stagiaire.setEtablissement(data.get("etablissement"));
            }

            utilisateurRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Utilisateur modifié avec succès"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ===== ACTIVER / DÉSACTIVER UN COMPTE =====

    @PutMapping("/utilisateurs/{id}/activer")
    public ResponseEntity<?> activerCompte(@PathVariable Long id) {
        return utilisateurRepository.findById(id).map(user -> {
            user.setActif(true);
            utilisateurRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Compte activé"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/utilisateurs/{id}/desactiver")
    public ResponseEntity<?> desactiverCompte(@PathVariable Long id) {
        return utilisateurRepository.findById(id).map(user -> {
            user.setActif(false);
            utilisateurRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Compte désactivé"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ===== DEMANDES D'ACCÈS =====

    @GetMapping("/demandes-acces")
    public ResponseEntity<List<DemandeAcces>> getDemandesEnAttente() {
        return ResponseEntity.ok(demandeAccesRepository.findByStatut(StatutDemande.EN_ATTENTE));
    }

    @GetMapping("/demandes-acces/toutes")
    public ResponseEntity<List<DemandeAcces>> getToutesDemandes() {
        return ResponseEntity.ok(demandeAccesRepository.findAll());
    }

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

        String motDePasse = UUID.randomUUID().toString().substring(0, 8);

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

        emailService.envoyerIdentifiants(
                demande.getEmail(),
                demande.getNom(),
                demande.getPrenom(),
                demande.getEmail(),
                motDePasse,
                demande.getRoleSouhaite()
        );

        demande.setStatut(StatutDemande.VALIDEE);
        demandeAccesRepository.save(demande);

        return ResponseEntity.ok(Map.of("message", "Demande validée, compte créé et email envoyé"));
    }

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

    // ===== GESTION UTILISATEURS =====

    @GetMapping("/utilisateurs")
    public ResponseEntity<List<Utilisateur>> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }

    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<?> supprimerUtilisateur(@PathVariable Long id) {
        utilisateurRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }
}
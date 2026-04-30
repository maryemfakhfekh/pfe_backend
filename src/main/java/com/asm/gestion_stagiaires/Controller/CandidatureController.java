package com.asm.gestion_stagiaires.Controller;

import com.asm.gestion_stagiaires.models.Candidature;
import com.asm.gestion_stagiaires.models.Utilisateur;
import com.asm.gestion_stagiaires.services.CandidatureService;
import com.asm.gestion_stagiaires.services.FileStorageService;
import com.asm.gestion_stagiaires.services.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidatures")
@CrossOrigin("*")
public class CandidatureController {

    @Autowired private CandidatureService candidatureService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private StageService stageService;

    // ===== STAGIAIRE =====

    @PostMapping("/postuler")
    @PreAuthorize("hasAuthority('ROLE_STAGIAIRE')")
    public ResponseEntity<Candidature> postuler(
            @RequestParam("File") MultipartFile file,
            @RequestParam("sujetId") Long sujetId,
            Principal principal) {

        String fileName = fileStorageService.save(file);
        Candidature candidature = new Candidature();
        candidature.setCvPath(fileName);

        return ResponseEntity.ok(
                candidatureService.saveCandidature(candidature, sujetId, principal.getName())
        );
    }

    @GetMapping("/has-accepted")
    @PreAuthorize("hasAuthority('ROLE_STAGIAIRE')")
    public ResponseEntity<Boolean> hasAcceptedCandidature(Principal principal) {
        Utilisateur utilisateur = candidatureService.getUtilisateurByEmail(principal.getName());
        boolean hasAccepted = candidatureService.hasAcceptedCandidature(utilisateur.getId());
        return ResponseEntity.ok(hasAccepted);
    }

    // ===== LECTURE COMMUNE =====

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_STAGIAIRE') or hasAuthority('ROLE_ADMIN')")
    public List<Candidature> voirCandidatures(
            @RequestParam(required = false) Long stagiaireId,
            Principal principal) {

        if (stagiaireId != null) {
            return candidatureService.getCandidaturesByStagiaire(stagiaireId);
        }

        if (principal.getName().equals("admin@asm.com")) {
            return candidatureService.getAllCandidatures();
        }

        return candidatureService.getAllCandidaturesByRh(principal.getName());
    }

    // ===== RH : PLANIFIER ENTRETIEN AVEC ENCADRANT =====
    // ✅ MODIFIÉ : prend dateEntretien + encadrantId
    @PutMapping("/{id}/entretien")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> planifierEntretien(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            LocalDateTime dateEntretien = LocalDateTime.parse(body.get("dateEntretien").toString());
            Long encadrantId = Long.valueOf(body.get("encadrantId").toString());
            return ResponseEntity.ok(
                    candidatureService.planifierEntretien(id, dateEntretien, encadrantId)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ===== ENCADRANT : VOIR SES ENTRETIENS =====

    @GetMapping("/mes-entretiens")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<List<Candidature>> getMesEntretiens(Principal principal) {
        return ResponseEntity.ok(candidatureService.getMesEntretiens(principal.getName()));
    }

    // ===== ENCADRANT : VALIDER APRÈS ENTRETIEN =====

    @PutMapping("/{id}/valider-encadrant")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<?> validerParEncadrant(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal) {
        try {
            String commentaire = (body != null) ? body.getOrDefault("commentaire", "") : "";
            return ResponseEntity.ok(
                    candidatureService.validerParEncadrant(id, principal.getName(), commentaire)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ===== ENCADRANT : REFUSER APRÈS ENTRETIEN =====

    @PutMapping("/{id}/refuser-encadrant")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<?> refuserParEncadrant(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal) {
        try {
            String commentaire = (body != null) ? body.getOrDefault("commentaire", "") : "";
            return ResponseEntity.ok(
                    candidatureService.refuserParEncadrant(id, principal.getName(), commentaire)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ===== RH : ACCEPTATION DÉFINITIVE =====

    @PutMapping("/{id}/accepter")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> accepter(@PathVariable Long id) {
        try {
            Candidature candidature = candidatureService.accepterCandidature(id);
            // Création du stage avec l'encadrant déjà assigné dans la candidature
            stageService.creerStageDepuisCandidature(candidature);
            return ResponseEntity.ok(candidature);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ===== RH : REFUS DÉFINITIF =====

    @PutMapping("/{id}/refuser")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> refuser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(candidatureService.refuserCandidature(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ===== SUPPRESSION =====

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        candidatureService.supprimerCandidature(id);
        return ResponseEntity.noContent().build();
    }
}
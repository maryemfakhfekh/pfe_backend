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
    @Autowired private StageService stagiaireService;

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

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_STAGIAIRE')")
    public List<Candidature> voirCandidatures(
            @RequestParam(required = false) Long stagiaireId,
            Principal principal) {

        if (stagiaireId != null) {
            return candidatureService.getCandidaturesByStagiaire(stagiaireId);
        }

        return candidatureService.getAllCandidaturesByRh(principal.getName());
    }

    @PutMapping("/{id}/accepter")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<Candidature> accepter(@PathVariable Long id) {
        Candidature candidature = candidatureService.accepterCandidature(id);
        stagiaireService.creerStage(candidature.getStagiaire(), candidature);
        return ResponseEntity.ok(candidature);
    }

    @PutMapping("/{id}/refuser")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<Candidature> refuser(@PathVariable Long id) {
        return ResponseEntity.ok(candidatureService.refuserCandidature(id));
    }

    @PutMapping("/{id}/entretien")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<Candidature> planifierEntretien(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        LocalDateTime dateEntretien = LocalDateTime.parse(body.get("dateEntretien"));
        return ResponseEntity.ok(candidatureService.planifierEntretien(id, dateEntretien));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        candidatureService.supprimerCandidature(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/has-accepted")
    @PreAuthorize("hasAuthority('ROLE_STAGIAIRE')")
    public ResponseEntity<Boolean> hasAcceptedCandidature(Principal principal) {
        Utilisateur utilisateur = candidatureService.getUtilisateurByEmail(principal.getName());
        boolean hasAccepted = candidatureService.hasAcceptedCandidature(utilisateur.getId());
        return ResponseEntity.ok(hasAccepted);
    }
}
package com.asm.gestion_stagiaires.Controller;

import com.asm.gestion_stagiaires.models.Stage;
import com.asm.gestion_stagiaires.models.Utilisateur;
import com.asm.gestion_stagiaires.repositories.StageRepository;
import com.asm.gestion_stagiaires.repositories.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/encadrants")
@CrossOrigin("*")
public class EncadrantController {

    @Autowired
    private StageRepository stagiaireRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @GetMapping("/mes-stagiaires")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<List<Stage>> getMesStagiaires(Principal principal) {
        Utilisateur encadrant = utilisateurRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Encadrant non trouvé"));
        List<Stage> stagiaires = stagiaireRepository.findByEncadrantId(encadrant.getId());
        return ResponseEntity.ok(stagiaires);
    }
}
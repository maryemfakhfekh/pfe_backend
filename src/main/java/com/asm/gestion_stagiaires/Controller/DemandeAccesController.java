package com.asm.gestion_stagiaires.Controller;

import com.asm.gestion_stagiaires.models.DemandeAcces;
import com.asm.gestion_stagiaires.repositories.DemandeAccesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demandes-acces")
@CrossOrigin("*")
public class DemandeAccesController {

    @Autowired
    private DemandeAccesRepository demandeAccesRepository;

    @PostMapping
    public ResponseEntity<?> soumettreDemande(@RequestBody DemandeAcces demande) {
        if (demandeAccesRepository.existsByEmail(demande.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Une demande avec cet email existe déjà"));
        }
        demande.setStatut(com.asm.gestion_stagiaires.models.StatutDemande.EN_ATTENTE);
        demandeAccesRepository.save(demande);
        return ResponseEntity.ok(Map.of("message", "Demande d'accès soumise avec succès"));
    }
}
package com.asm.gestion_stagiaires.Controller;

import com.asm.gestion_stagiaires.dto.SujetStageDTO;
import com.asm.gestion_stagiaires.models.SujetStage;
import com.asm.gestion_stagiaires.models.Utilisateur;
import com.asm.gestion_stagiaires.repositories.SujetStageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sujets")
@CrossOrigin("*")
public class SujetStageController {

    @Autowired
    private SujetStageRepository sujetStageRepository;

    private SujetStageDTO toDTO(SujetStage s) {
        SujetStageDTO dto = new SujetStageDTO();
        dto.setId(s.getId());
        dto.setTitre(s.getTitre());
        dto.setDescription(s.getDescription());
        dto.setFiliereCible(s.getFiliereCible());
        dto.setCycleCible(s.getCycleCible());
        dto.setCompetencesCibles(s.getCompetencesCibles());
        dto.setDatePublication(s.getDatePublication());
        dto.setEstDisponible(s.getEstDisponible());
        if (s.getCreateur() != null) {
            dto.setCreateurId(s.getCreateur().getId());
            dto.setCreateurNom(s.getCreateur().getNom() + " " + s.getCreateur().getPrenom());        }
        return dto;
    }

    // ✅ NOUVEAU : Sujets disponibles
    @GetMapping("/disponibles")
    public ResponseEntity<List<SujetStageDTO>> voirSujetsDisponibles() {
        List<SujetStageDTO> offres = sujetStageRepository
                .findByEstDisponibleTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(offres);
    }

    // POST : Créer un sujet
    @PostMapping("/publier")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<SujetStageDTO> creerSujet(@RequestBody SujetStage sujet,
                                                    @AuthenticationPrincipal Utilisateur createur) {
        if (createur == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        sujet.setCreateur(createur);
        SujetStage saved = sujetStageRepository.save(sujet);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    // GET : Mes offres (RH)
    @GetMapping("/mes-offres")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<List<SujetStageDTO>> voirMesSujets(@AuthenticationPrincipal Utilisateur createur) {
        if (createur == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<SujetStageDTO> offres = sujetStageRepository.findByCreateurId(createur.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(offres);
    }

    // GET : Tous les sujets
    @GetMapping
    public ResponseEntity<List<SujetStageDTO>> voirLesSujets(@RequestParam(required = false) String cycle) {
        List<SujetStage> offres;
        if (cycle != null && !cycle.isEmpty()) {
            offres = sujetStageRepository.findByCycleCible(cycle);
        } else {
            offres = sujetStageRepository.findAll();
        }
        return ResponseEntity.ok(offres.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    // GET : Sujet par ID
    @GetMapping("/{id}")
    public ResponseEntity<SujetStageDTO> voirSujetParId(@PathVariable Long id) {
        return sujetStageRepository.findById(id)
                .map(s -> ResponseEntity.ok(toDTO(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT : Modifier un sujet
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<SujetStageDTO> modifierSujet(@PathVariable Long id,
                                                       @RequestBody SujetStage sujetDetails,
                                                       @AuthenticationPrincipal Utilisateur createur) {
        if (createur == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var optionalSujet = sujetStageRepository.findById(id);
        if (optionalSujet.isEmpty()) return ResponseEntity.notFound().build();
        var sujet = optionalSujet.get();
        if (!sujet.getCreateur().getId().equals(createur.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        sujet.setTitre(sujetDetails.getTitre());
        sujet.setDescription(sujetDetails.getDescription());
        sujet.setFiliereCible(sujetDetails.getFiliereCible());
        sujet.setCycleCible(sujetDetails.getCycleCible());
        sujet.setCompetencesCibles(sujetDetails.getCompetencesCibles());
        sujet.setEstDisponible(sujetDetails.getEstDisponible());
        return ResponseEntity.ok(toDTO(sujetStageRepository.save(sujet)));
    }

    // DELETE : Supprimer un sujet
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<Void> supprimerSujet(@PathVariable Long id,
                                               @AuthenticationPrincipal Utilisateur createur) {
        if (createur == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var optionalSujet = sujetStageRepository.findById(id);
        if (optionalSujet.isEmpty()) return ResponseEntity.notFound().build();
        var sujet = optionalSujet.get();
        if (!sujet.getCreateur().getId().equals(createur.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        sujetStageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
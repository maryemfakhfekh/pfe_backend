package com.asm.gestion_stagiaires.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Candidature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cvPath;

    // --- Sprint 2 : Analyse IA ---
    private Double scoreMatchingIA;

    @ElementCollection
    private List<String> competencesExtraites;

    // --- Attributs de Gestion ---
    @Enumerated(EnumType.STRING)
    private StatusCandidature statut = StatusCandidature.EN_ATTENTE;

    private LocalDate dateDepot = LocalDate.now();

    // --- Attribut pour l'Entretien ---
    private LocalDateTime dateEntretien;

    // ✅ Commentaire de l'encadrant après l'entretien
    @Column(columnDefinition = "TEXT")
    private String commentaireEncadrant;

    // --- Relations ---
    @ManyToOne
    @JoinColumn(name = "stagiaire_id")
    private Utilisateur stagiaire;

    @ManyToOne
    @JoinColumn(name = "sujet_id")
    private SujetStage sujet;

    // ✅ NOUVEAU : Encadrant assigné pour l'entretien
    @ManyToOne
    @JoinColumn(name = "encadrant_id")
    private Utilisateur encadrant;
}
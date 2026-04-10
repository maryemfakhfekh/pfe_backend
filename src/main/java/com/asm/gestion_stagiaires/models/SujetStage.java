package com.asm.gestion_stagiaires.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SujetStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String filiereCible;

    private String cycleCible;

    @ElementCollection
    private List<String> competencesCibles;

    @Temporal(TemporalType.DATE)
    private Date datePublication = new Date();

    private Boolean estDisponible = true;

    // --- AJOUT POUR LA ROBUSTESSE ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rh")
    @JsonIgnore  // ← Empêche la sérialisation du proxy Hibernate
    private Utilisateur createur;
    // --------------------------------
}
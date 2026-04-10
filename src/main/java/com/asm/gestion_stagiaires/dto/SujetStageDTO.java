package com.asm.gestion_stagiaires.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SujetStageDTO {
    private Long id;
    private String titre;
    private String description;
    private String filiereCible;
    private String cycleCible;
    private List<String> competencesCibles;
    private Date datePublication;
    private Boolean estDisponible;
    // ✅ Infos du créateur exposées proprement
    private Long createurId;
    private String createurNom;
}
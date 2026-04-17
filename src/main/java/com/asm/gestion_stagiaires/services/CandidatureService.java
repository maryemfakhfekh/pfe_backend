package com.asm.gestion_stagiaires.services;

import com.asm.gestion_stagiaires.models.Candidature;
import com.asm.gestion_stagiaires.models.StatusCandidature;
import com.asm.gestion_stagiaires.models.SujetStage;
import com.asm.gestion_stagiaires.models.Utilisateur;
import com.asm.gestion_stagiaires.repositories.CandidatureRepository;
import com.asm.gestion_stagiaires.repositories.SujetStageRepository;
import com.asm.gestion_stagiaires.repositories.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CandidatureService {

    @Autowired private CandidatureRepository candidatureRepository;
    @Autowired private SujetStageRepository sujetStageRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;

    public Candidature saveCandidature(Candidature candidature, Long sujetId, String email) {
        SujetStage sujet = sujetStageRepository.findById(sujetId)
                .orElseThrow(() -> new RuntimeException("Sujet non trouvé"));
        Utilisateur stagiaire = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));
        candidature.setSujet(sujet);
        candidature.setStagiaire(stagiaire);
        candidature.setStatut(StatusCandidature.EN_ATTENTE);
        candidature.setDateDepot(LocalDate.now());
        return candidatureRepository.save(candidature);
    }

    public List<Candidature> getAllCandidaturesByRh(String emailRh) {
        Utilisateur rh = utilisateurRepository.findByEmail(emailRh)
                .orElseThrow(() -> new RuntimeException("RH non trouvé"));
        List<SujetStage> mesSujets = sujetStageRepository.findByCreateur(rh);
        return candidatureRepository.findBySujetIn(mesSujets);
    }

    // ✅ Toutes les candidatures — pour l'admin
    public List<Candidature> getAllCandidatures() {
        return candidatureRepository.findAll();
    }

    public List<Candidature> getCandidaturesByStagiaire(Long stagiaireId) {
        return candidatureRepository.findByStagiaireId(stagiaireId);
    }

    public List<Candidature> getCandidaturesParStatut(StatusCandidature statut) {
        return candidatureRepository.findByStatut(statut);
    }

    public boolean hasAcceptedCandidature(Long stagiaireId) {
        return candidatureRepository.existsByStagiaireIdAndStatut(stagiaireId, StatusCandidature.ACCEPTE);
    }

    public Utilisateur getUtilisateurByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    public Candidature accepterCandidature(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));
        candidature.setStatut(StatusCandidature.ACCEPTE);
        return candidatureRepository.save(candidature);
    }

    public Candidature refuserCandidature(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));
        candidature.setStatut(StatusCandidature.REFUSEE);
        return candidatureRepository.save(candidature);
    }

    public Candidature planifierEntretien(Long id, LocalDateTime dateEntretien) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));
        candidature.setDateEntretien(dateEntretien);
        return candidatureRepository.save(candidature);
    }

    public void supprimerCandidature(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));
        candidatureRepository.delete(candidature);
    }
}
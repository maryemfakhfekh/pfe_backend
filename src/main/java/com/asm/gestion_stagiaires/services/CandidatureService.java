package com.asm.gestion_stagiaires.services;

import com.asm.gestion_stagiaires.models.Candidature;
import com.asm.gestion_stagiaires.models.Encadrant;
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
    @Autowired private EmailService emailService;

    // ===== CRÉATION =====

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

    // ===== LECTURE =====

    public List<Candidature> getAllCandidaturesByRh(String emailRh) {
        Utilisateur rh = utilisateurRepository.findByEmail(emailRh)
                .orElseThrow(() -> new RuntimeException("RH non trouvé"));
        List<SujetStage> mesSujets = sujetStageRepository.findByCreateur(rh);
        return candidatureRepository.findBySujetIn(mesSujets);
    }

    public List<Candidature> getAllCandidatures() {
        return candidatureRepository.findAll();
    }

    public List<Candidature> getCandidaturesByStagiaire(Long stagiaireId) {
        return candidatureRepository.findByStagiaireId(stagiaireId);
    }

    public List<Candidature> getCandidaturesParStatut(StatusCandidature statut) {
        return candidatureRepository.findByStatut(statut);
    }

    // ✅ NOUVEAU : entretiens assignés à un encadrant
    public List<Candidature> getMesEntretiens(String emailEncadrant) {
        Utilisateur encadrant = utilisateurRepository.findByEmail(emailEncadrant)
                .orElseThrow(() -> new RuntimeException("Encadrant non trouvé"));
        return candidatureRepository.findByEncadrantId(encadrant.getId());
    }

    public boolean hasAcceptedCandidature(Long stagiaireId) {
        return candidatureRepository.existsByStagiaireIdAndStatut(stagiaireId, StatusCandidature.ACCEPTE);
    }

    public Utilisateur getUtilisateurByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    // ===== RH : PLANIFIER ENTRETIEN AVEC ENCADRANT =====

    public Candidature planifierEntretien(Long id, LocalDateTime dateEntretien, Long encadrantId) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        if (candidature.getStatut() != StatusCandidature.EN_ATTENTE) {
            throw new RuntimeException("L'entretien ne peut être planifié que pour une candidature EN_ATTENTE");
        }

        Utilisateur encadrant = utilisateurRepository.findById(encadrantId)
                .orElseThrow(() -> new RuntimeException("Encadrant non trouvé"));

        if (!(encadrant instanceof Encadrant)) {
            throw new RuntimeException("L'utilisateur sélectionné n'est pas un encadrant");
        }

        candidature.setDateEntretien(dateEntretien);
        candidature.setEncadrant(encadrant);
        candidature.setStatut(StatusCandidature.EN_ENTRETIEN);
        Candidature saved = candidatureRepository.save(candidature);

        // Emails
        Utilisateur stagiaire = candidature.getStagiaire();
        String sujetTitre = candidature.getSujet() != null
                ? candidature.getSujet().getTitre()
                : "votre stage";

        try {
            // Email au stagiaire
            emailService.envoyerEmailEntretien(
                    stagiaire.getEmail(),
                    stagiaire.getNom(),
                    stagiaire.getPrenom(),
                    encadrant.getNom(),
                    encadrant.getPrenom(),
                    "Encadrant",
                    dateEntretien,
                    sujetTitre
            );

            // Email à l'encadrant
            emailService.envoyerEmailEntretien(
                    encadrant.getEmail(),
                    encadrant.getNom(),
                    encadrant.getPrenom(),
                    stagiaire.getNom(),
                    stagiaire.getPrenom(),
                    "Stagiaire",
                    dateEntretien,
                    sujetTitre
            );
        } catch (Exception e) {
            System.err.println("Erreur envoi email entretien : " + e.getMessage());
        }

        return saved;
    }

    // ===== ENCADRANT : VALIDER APRÈS ENTRETIEN =====

    public Candidature validerParEncadrant(Long id, String emailEncadrant, String commentaire) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        if (candidature.getStatut() != StatusCandidature.EN_ENTRETIEN) {
            throw new RuntimeException("Cette candidature n'est pas en phase d'entretien");
        }

        // Vérifier que c'est bien l'encadrant assigné
        if (candidature.getEncadrant() == null
                || !candidature.getEncadrant().getEmail().equals(emailEncadrant)) {
            throw new RuntimeException("Vous n'êtes pas l'encadrant assigné à cette candidature");
        }

        candidature.setStatut(StatusCandidature.VALIDEE_ENCADRANT);
        candidature.setCommentaireEncadrant(commentaire);
        Candidature saved = candidatureRepository.save(candidature);

        // Email au RH
        Utilisateur stagiaire = candidature.getStagiaire();
        Utilisateur encadrant = candidature.getEncadrant();
        String sujetTitre = candidature.getSujet() != null
                ? candidature.getSujet().getTitre()
                : "stage";

        try {
            emailService.envoyerEmailValidationEncadrantAuxRH(
                    stagiaire.getNom(), stagiaire.getPrenom(),
                    encadrant.getNom(), encadrant.getPrenom(),
                    sujetTitre, commentaire
            );
        } catch (Exception e) {
            System.err.println("Erreur envoi email validation encadrant : " + e.getMessage());
        }

        return saved;
    }

    // ===== ENCADRANT : REFUSER APRÈS ENTRETIEN =====

    public Candidature refuserParEncadrant(Long id, String emailEncadrant, String commentaire) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        if (candidature.getStatut() != StatusCandidature.EN_ENTRETIEN) {
            throw new RuntimeException("Cette candidature n'est pas en phase d'entretien");
        }

        if (candidature.getEncadrant() == null
                || !candidature.getEncadrant().getEmail().equals(emailEncadrant)) {
            throw new RuntimeException("Vous n'êtes pas l'encadrant assigné à cette candidature");
        }

        candidature.setStatut(StatusCandidature.REFUSEE_ENCADRANT);
        candidature.setCommentaireEncadrant(commentaire);
        Candidature saved = candidatureRepository.save(candidature);

        // Email au RH
        Utilisateur stagiaire = candidature.getStagiaire();
        Utilisateur encadrant = candidature.getEncadrant();
        String sujetTitre = candidature.getSujet() != null
                ? candidature.getSujet().getTitre()
                : "stage";

        try {
            emailService.envoyerEmailRefusEncadrantAuxRH(
                    stagiaire.getNom(), stagiaire.getPrenom(),
                    encadrant.getNom(), encadrant.getPrenom(),
                    sujetTitre, commentaire
            );
        } catch (Exception e) {
            System.err.println("Erreur envoi email refus encadrant : " + e.getMessage());
        }

        return saved;
    }

    // ===== RH : ACCEPTATION DÉFINITIVE =====

    public Candidature accepterCandidature(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        // ✅ Le RH ne peut accepter que si l'encadrant a validé
        if (candidature.getStatut() != StatusCandidature.VALIDEE_ENCADRANT) {
            throw new RuntimeException(
                    "L'acceptation définitive nécessite la validation préalable de l'encadrant"
            );
        }

        candidature.setStatut(StatusCandidature.ACCEPTE);
        Candidature saved = candidatureRepository.save(candidature);

        Utilisateur stagiaire = candidature.getStagiaire();
        String sujetTitre = candidature.getSujet() != null
                ? candidature.getSujet().getTitre()
                : "votre stage";

        try {
            emailService.envoyerEmailCandidatureAcceptee(
                    stagiaire.getEmail(),
                    stagiaire.getNom(),
                    stagiaire.getPrenom(),
                    sujetTitre
            );
        } catch (Exception e) {
            System.err.println("Erreur envoi email acceptation : " + e.getMessage());
        }

        return saved;
    }

    // ===== RH : REFUS DÉFINITIF =====
    // Possible à n'importe quelle étape (EN_ATTENTE, EN_ENTRETIEN, VALIDEE/REFUSEE_ENCADRANT)

    public Candidature refuserCandidature(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        if (candidature.getStatut() == StatusCandidature.ACCEPTE
                || candidature.getStatut() == StatusCandidature.REFUSEE) {
            throw new RuntimeException("Cette candidature est déjà finalisée");
        }

        candidature.setStatut(StatusCandidature.REFUSEE);
        Candidature saved = candidatureRepository.save(candidature);

        Utilisateur stagiaire = candidature.getStagiaire();
        String sujetTitre = candidature.getSujet() != null
                ? candidature.getSujet().getTitre()
                : "votre stage";

        try {
            emailService.envoyerEmailCandidatureRefusee(
                    stagiaire.getEmail(),
                    stagiaire.getNom(),
                    stagiaire.getPrenom(),
                    sujetTitre
            );
        } catch (Exception e) {
            System.err.println("Erreur envoi email refus : " + e.getMessage());
        }

        return saved;
    }

    // ===== SUPPRESSION =====

    public void supprimerCandidature(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));
        candidatureRepository.delete(candidature);
    }
}
package com.asm.gestion_stagiaires.repositories;

import com.asm.gestion_stagiaires.models.SujetStage;
import com.asm.gestion_stagiaires.models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SujetStageRepository extends JpaRepository<SujetStage, Long> {

    // IMPORTANT : Récupère uniquement les sujets créés par un RH spécifique
    List<SujetStage> findByCreateurId(Long rhId);
    List<SujetStage> findByCreateur(Utilisateur createur);
    // Filtrage par cycle (Master / Ingénierie) pour les stagiaires
    List<SujetStage> findByCycleCible(String cycleCible);
    List<SujetStage> findByEstDisponibleTrue(); // ✅ NOUVEAU

    // Filtrage combiné pour une recherche précise
    List<SujetStage> findByCycleCibleAndFiliereCible(String cycleCible, String filiereCible);
}
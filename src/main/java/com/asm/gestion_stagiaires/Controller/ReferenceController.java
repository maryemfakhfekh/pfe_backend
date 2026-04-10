package com.asm.gestion_stagiaires.Controller;

import com.asm.gestion_stagiaires.models.Filiere;
import com.asm.gestion_stagiaires.models.Cycle;
import com.asm.gestion_stagiaires.repositories.FiliereRepository;
import com.asm.gestion_stagiaires.repositories.CycleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/references")
public class ReferenceController {

    @Autowired
    private FiliereRepository filiereRepository;

    @Autowired
    private CycleRepository cycleRepository;

    @GetMapping("/filieres")
    public List<Filiere> getFilieres() {
        return filiereRepository.findAll();
    }

    @GetMapping("/cycles")
    public List<Cycle> getCycles() {
        return cycleRepository.findAll();
    }
}
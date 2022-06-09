package com.example.my_activity_server;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

public class NaturalLangExplainer {

    public void getNatLangExplanations(Set<OWLAxiom> explanations) {
        explanations.forEach(ex -> System.out.println(ex));
    }

}
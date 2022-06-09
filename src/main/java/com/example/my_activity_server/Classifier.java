package com.example.my_activity_server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.explanation.PelletExplanation;

public class Classifier {
    public static Map<OWLNamedIndividual, Set<OWLAxiom>> classifyIndividuals(OWLClass targetActivityClass,
            OpenlletReasoner reasoner) {

        // classify activity instances
        Set<OWLNamedIndividual> individuals = reasoner.getInstances(targetActivityClass, true).getFlattened();
        final PelletExplanation explainer = new PelletExplanation(reasoner);

        // create explanations
        Map<OWLNamedIndividual, Set<OWLAxiom>> classificationResult = new HashMap<>();
        individuals.forEach(ind -> {
            Set<OWLAxiom> axs = explainer.getInstanceExplanation(ind, targetActivityClass);
            classificationResult.put(ind, axs);
        });

        return classificationResult;
    }
}

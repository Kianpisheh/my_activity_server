package com.example.my_activity_server;

import java.io.File;
import java.io.PrintWriter;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import openllet.owlapi.explanation.PelletExplanation;
import openllet.owlapi.explanation.io.manchester.ManchesterSyntaxExplanationRenderer;

public class App {
    public static void main(String[] args) {
        PelletExplanation.setup();
        // The renderer is used to pretty print clashExplanation
        final ManchesterSyntaxExplanationRenderer renderer = new ManchesterSyntaxExplanationRenderer();
        // The writer used for the clashExplanation rendered
        final PrintWriter out = new PrintWriter(System.out);
        renderer.startRendering(out);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        // setup and load the ontolgy
        File file = new File("act_ont_015.owl");
        try {
            ontology = manager.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException ee) {
            ee.printStackTrace();
            return;
        }
        String ontIRI = "";

        try {
            ontIRI = ontology.getOntologyID().getOntologyIRI().orElseThrow(Exception::new).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        PrefixManager pm = new DefaultPrefixManager(ontIRI + "#");

        // create and modify activities
        ontology.axioms().forEach(axiom -> {
            if (axiom instanceof SWRLRule) {
                OWLActivity activity = new OWLActivity((SWRLRule) axiom);
            }
        });

        // single instance classification
        OWLNamedIndividual ind1 = manager.getOWLDataFactory()
                .getOWLNamedIndividual(":activity_01", pm);

        UserExplainer.explain(ind1, ActivityList.Brushing_TEETH, "why", manager, ontology,
                pm);
    }
}
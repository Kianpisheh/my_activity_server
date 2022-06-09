package com.example.my_activity_server.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class ServiceUtils {

    public static Map<String, Object> ontologyAssetsSetup(String filename, OWLOntologyManager manager) {
        OWLOntology ontology = null;
        String ontIRI = "";
        PrefixManager pm = null;

        File file = new File(filename);
        try {
            ontology = manager.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyAlreadyExistsException e) {
            e.printStackTrace();
        } catch (OWLOntologyCreationException ee) {
            ee.printStackTrace();
            return null;
        }

        try {
            ontIRI = ontology.getOntologyID().getOntologyIRI().orElseThrow(Exception::new).toString();
        } catch (OWLOntologyAlreadyExistsException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        pm = new DefaultPrefixManager(ontIRI + "#");

        Map<String, Object> output = new HashMap<>();
        output.put("pm", pm);
        output.put("ontology", ontology);

        return output;

    }

    public static OWLOntology getOntology(String filename, OWLOntologyManager manager) {
        Map<String, Object> output = ontologyAssetsSetup(filename, manager);
        OWLOntology ontology = (OWLOntology) output.get("ontology");
        return ontology;
    }
}

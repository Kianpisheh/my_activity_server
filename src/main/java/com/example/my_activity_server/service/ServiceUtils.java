package com.example.my_activity_server.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.ByteArrayInputStream;

import org.bson.Document;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import com.example.my_activity_server.model.ActivityInstance;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class ServiceUtils {

    public static Map<String, Object> ontologyAssetsSetup(OWLOntologyManager manager, MongoCollection col) {
        OWLOntology ontology = null;
        String ontIRI = "";
        PrefixManager pm = null;

        try {
            Document d = (Document) col.find(new Document("_id", "12")).first();
            String ontText = (String) d.get("ontText");
            InputStream stream = new ByteArrayInputStream(ontText.getBytes(StandardCharsets.UTF_8));
            ontology = manager.loadOntologyFromOntologyDocument(stream);
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

    public static OWLOntology getOntology(MongoCollection col, OWLOntologyManager manager) {
        Map<String, Object> output = ontologyAssetsSetup(manager, col);
        OWLOntology ontology = (OWLOntology) output.get("ontology");
        return ontology;
    }

    // finding the frequent rules
    public List<String> getFrquentEventsets(List<ActivityInstance> instances, float minSup, float minConf) {
        List<String> freqEvents = new ArrayList<>();

        return freqEvents;
    }

}

package com.example.my_activity_server.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

import java.nio.charset.StandardCharsets;

import org.bson.types.ObjectId;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.example.my_activity_server.model.ActivityInstance;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

public class ServiceUtils {

    public static Map<String, Object> ontologyAssetsSetup(OWLOntologyManager manager, MongoCollection col,
            String dataset, String ontologySource) {

        OWLOntology ontology = null;
        String ontIRI = "";
        PrefixManager pm = null;

        // load from local data
        if (ontologySource.equals("local")) {
            File f = new File("../ontologies/" + dataset + "_ontology.owl");
            try {
                ontology = manager.loadOntologyFromOntologyDocument(f);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (ontologySource.equals("mongo")) {
            // database setup
            try {
                Document ontDoc = (Document) col.find(eq("name", dataset + "_ontology")).first();
                String ontText = (String) ontDoc.get("ontText");
                InputStream stream = new ByteArrayInputStream(ontText.getBytes(StandardCharsets.UTF_8));
                // manager.clearOntologies();
                ontology = manager.loadOntologyFromOntologyDocument(stream);
            } catch (OWLOntologyAlreadyExistsException e) {
                e.printStackTrace();
            } catch (OWLOntologyCreationException ee) {
                ee.printStackTrace();
                return null;
            }

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

    public static void saveOntology(OWLOntology ontology, String dataset, MongoCollection col, String ontologySource) {

        // save to local file
        // File f = null;
        // // load from local data
        // if (dataset.contains("CASAS8")) {
        // f = new File("../ontologies/CASAS8_ontology.owl");
        // } else if (dataset.equals("Opportunity")) {
        // f = new File("../ontologies/Opportunity_ontology.owl");
        // } else if (dataset.equals("Opportunity_gesture")) {
        // f = new File("../ontologies/Opportunity_gesture_ontology.owl");
        // }

        if (ontologySource.equals("local")) {
            try {
                ontology.saveOntology();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (ontologySource.equals("mongo")) {

            // save to the database
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ontology.saveOntology(ontology.getFormat(), outputStream);
                // String ontText = outputStream.toString(StandardCharsets.UTF_8);
                String ontText = new String(outputStream.toByteArray(), "UTF-8");
                Document newDoc = new Document("name", dataset).append("ontText", ontText);
                Bson query = Filters.eq("name", dataset);
                col.replaceOne(query, newDoc);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // finding the frequent rules
    public List<String> getFrquentEventsets(List<ActivityInstance> instances, float minSup, float minConf) {
        List<String> freqEvents = new ArrayList<>();

        return freqEvents;
    }

}

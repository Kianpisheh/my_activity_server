package com.example.my_activity_server.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.my_activity_server.ActivityInstanceManager;
import com.example.my_activity_server.Predicate;
import com.example.my_activity_server.model.ActivityInstance;
import com.example.my_activity_server.model.EventInstance;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.bson.conversions.Bson;
import static com.mongodb.client.model.Filters.eq;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;

@Service
public class ActivityInstanceService {
    OWLOntologyManager manager = null;
    OWLOntology ontology = null;
    PrefixManager pm = null;
    String ontIRI = "";

    @Autowired
    private ActivityService activityService;

    public List<ActivityInstance> getActivityInstances() {

        ontology = activityService.getOntology();
        manager = activityService.getManager();
        pm = activityService.getPrefixManager();
        if (ontology == null) {
            Map<String, Object> ontologyPm = ServiceUtils.ontologyAssetsSetup(manager,
                    activityService.getDBCollection());
            ontology = (OWLOntology) ontologyPm.get("ontology");
            pm = (PrefixManager) ontologyPm.get("pm");
        }

        List<ActivityInstance> activityInstances = new ArrayList<>();
        File file = new File("./data");

        Gson gson = new Gson();
        String[] fileDirs = file.list();
        for (String dir : fileDirs) {
            if (!dir.endsWith(".json")) {
                continue;
            }

            Map<String, Object> object = null;
            try {
                object = (Map<String, Object>) gson.fromJson(new FileReader("./data/" + dir),
                        Object.class);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

            String actName = (String) object.get("name");
            String actType = (String) object.get("type");
            List<EventInstance> eventInstances = new ArrayList<>();
            List<Map<String, Object>> evs = (List<Map<String, Object>>) object.get("events");
            for (Map<String, Object> ev : evs) {
                String evType = (String) ev.get("type");
                double t1 = (double) ev.get("start_time");
                double t2 = (double) ev.get("end_time");
                String eventName = actName + "_" + evType + "_" + Double.toString(t2);
                eventInstances.add(new EventInstance(eventName, evType, t1, t2));
            }
            activityInstances.add(new ActivityInstance(actName, actType, eventInstances));

        }

        ontology = ActivityInstanceManager.addActivityInstances(activityInstances, manager, ontology, pm);
        String uri = "mongodb+srv://kian:mk89081315@cluster0.ekorb.mongodb.net/?retryWrites=true&w=majority";
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase db = mongoClient.getDatabase("HAKEE-database");
        MongoCollection col = db.getCollection("activity_ontology");

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ontology.saveOntology(ontology.getFormat(), outputStream);
            String ontText = outputStream.toString(StandardCharsets.UTF_8);
            Document newDoc = new Document().append("ontText", ontText);
            Bson query = eq("_id", "12");
            col.replaceOne(query, newDoc);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // activityService.setAndSaveOntology(ontology);

        return activityInstances;

    }

    public List<List<String>> classify(List<String> activityInstances) {
        PelletExplanation.setup();

        ontology = activityService.getOntology();
        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);

        List<List<String>> results = new ArrayList<>();

        // per instance
        for (String instanceName : activityInstances) {
            OWLNamedIndividual ind = manager.getOWLDataFactory()
                    .getOWLNamedIndividual(instanceName, pm);

            List<String> cls = new ArrayList<>();
            List<OWLClass> classes = new ArrayList<>(reasoner.getTypes(ind, true).getFlattened());
            for (OWLClass class_ : classes) {
                String activityName = class_.getIRI().getShortForm();
                if (activityName.equals(Predicate.ACTIVIY)) {
                    continue;
                }
                cls.add(class_.getIRI().getShortForm());
            }
            results.add(cls);
        }

        return results;
    }
}
package com.example.my_activity_server.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.example.my_activity_server.ActivityInstanceManager;
import com.example.my_activity_server.Predicate;
import com.example.my_activity_server.model.ActivityInstance;
import com.example.my_activity_server.model.EventInstance;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@Service
public class ActivityInstanceService {
    OWLOntologyManager manager = null;
    OWLOntology ontology = null;
    PrefixManager pm = null;
    String ontIRI = "";

    @Autowired
    private ActivityService activityService;

    public List<ActivityInstance> getActivityInstances(Map<String, String> dataset) {

        ontology = activityService.getOntology();
        manager = activityService.getManager();
        pm = activityService.getPrefixManager();
        if (ontology == null) {
            Map<String, Object> ontologyPm = ServiceUtils.ontologyAssetsSetup(manager,
                    activityService.getDBCollection(), dataset.get("dataset") + "-" + dataset.get("user"),
                    activityService.ontologySource);
            ontology = (OWLOntology) ontologyPm.get("ontology");
            pm = (PrefixManager) ontologyPm.get("pm");
        }

        // read data from local files
        // String path = "../data/" + dataset.get("dataset") + "/";
        // List<ActivityInstance> activityInstances = new ArrayList<>();
        // File folder = new File(path);

        // JSONParser parser = new JSONParser();
        // try {
        // Object obj = parser.parse(new FileReader("../data/" + ".json"));

        // File f = new File(path, child);

        // read data from the database
        Map<String, Object> object = null;
        List<ActivityInstance> activityInstances = new ArrayList<>();
        MongoDatabase db = activityService.getDatabase();
        MongoCollection activityCollection = db.getCollection(dataset.get("dataset")
                + "-dataset");
        FindIterable<Document> iterDoc = activityCollection.find();
        Iterator it = iterDoc.iterator();
        while (it.hasNext()) {
            object = (Map<String, Object>) it.next();
            String actName = (String) object.get("name");
            String actType = (String) object.get("type");
            List<EventInstance> eventInstances = new ArrayList<>();
            List<Map<String, Object>> evs = (List<Map<String, Object>>) object.get("events");
            if (evs == null) {
                continue;
            }
            for (Map<String, Object> ev : evs) {
                double t1, t2;
                String evType = (String) ev.get("type");
                if (ev.get("start_time") instanceof Double) {
                    t1 = (double) ev.get("start_time");
                    t2 = (double) ev.get("end_time");
                } else {
                    t1 = (int) ev.get("start_time");
                    t2 = (int) ev.get("end_time");
                }
                ;
                String eventName = actName + "_" + evType + "_" + Double.toString(t2);
                eventInstances.add(new EventInstance(eventName, evType, t1, t2));
            }
            activityInstances.add(new ActivityInstance(actName, actType,
                    eventInstances));
        }

        // add the activity and event indioviduals+the corresponding classes into the
        // ontology
        ontology = ActivityInstanceManager.addActivityInstances(activityInstances, manager, ontology, pm);
        MongoCollection col = activityService.getDBCollection();

        // save the ontology
        // Thread newThread = new Thread(() -> {
        // ServiceUtils.saveOntology(ontology, dataset.get("dataset") + "-" +
        // activityService.user, col,
        // activityService.ontologySource);
        // });
        // newThread.start();

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
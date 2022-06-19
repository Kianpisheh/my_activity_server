package com.example.my_activity_server.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.my_activity_server.ActivityInstanceManager;
import com.example.my_activity_server.Predicate;
import com.example.my_activity_server.model.ActivityInstance;
import com.example.my_activity_server.model.EventInstance;
import com.google.gson.Gson;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
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
            Map<String, Object> ontologyPm = ServiceUtils.ontologyAssetsSetup("act_ont_015.owl", manager);
            ontology = (OWLOntology) ontologyPm.get("ontology");
            pm = (PrefixManager) ontologyPm.get("pm");
        }

        List<ActivityInstance> activityInstances = new ArrayList<>();
        List<OWLIndividual> individuals = new ArrayList<>();
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
            List<EventInstance> eventInstances = new ArrayList<>();
            List<Map<String, Object>> evs = (List<Map<String, Object>>) object.get("events");
            for (Map<String, Object> ev : evs) {
                String evType = (String) ev.get("type");
                double t1 = (double) ev.get("start_time");
                double t2 = (double) ev.get("end_time");
                String eventName = actName + "_" + evType + "_" + Double.toString(t2);
                eventInstances.add(new EventInstance(eventName, evType, t1, t2));
            }
            activityInstances.add(new ActivityInstance(actName, "", eventInstances));

        }

        ontology = ActivityInstanceManager.addActivityInstances(activityInstances, manager, ontology, pm);
        activityService.setAndSaveOntology(ontology);

        return activityInstances;

    }

    public List<List<String>> classify(List<String> activityInstances) {
        PelletExplanation.setup();

        ontology = activityService.getOntology();
        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);

        List<List<String>> results = new ArrayList<>();

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

// Load the owl ontology
// find activity instances in the ontology
// ontology.axioms().forEach(axiom -> {
// if (axiom instanceof OWLClassAssertionAxiom) {
// OWLClassAssertionAxiom individualAxiom = (OWLClassAssertionAxiom) axiom;
// String classStr =
// individualAxiom.getClassExpression().asOWLClass().getIRI().getShortForm();
// if (classStr.equals(ActivityList.ROOT_ACTIVITY)) {
// individuals.add(individualAxiom.getIndividual());
// }
// }
// });

// individuals.forEach(ind -> {
// Set<OWLIndividualAxiom> indAxioms = ontology.getAxioms(ind);
// String instanceName = ind.asOWLNamedIndividual().getIRI().getShortForm();
// List<EventInstance> events = ActivityInstanceManager.getEvents(indAxioms,
// ontology);

// activityInstances.add(new ActivityInstance(instanceName, "", events));
// });

package com.example.my_activity_server.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.my_activity_server.ActivityList;
import com.example.my_activity_server.OWLActivity;
import com.example.my_activity_server.SWRLRuleFactory;
import com.example.my_activity_server.API.OwlToPojo;
import com.example.my_activity_server.API.PojoToOWL;
import com.example.my_activity_server.model.Activity;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = null;;
    PrefixManager pm = null;
    String ontIRI = "";
    int version = 0;

    public OWLOntology getOntology() {
        return ontology;
    }

    public void setAndSaveOntology(OWLOntology ontology_) {
        ontology = ontology_;
        IRI versionIRI = IRI.create(String.valueOf(version));
        SetOntologyID change = new SetOntologyID(ontology,
                new OWLOntologyID(ontology.getOntologyID().getOntologyIRI(), Optional.of(versionIRI)));
        version += 1;
        ontology.getOWLOntologyManager().applyChange(change);
        // save the ontology
        try {
            File fileout = new File("./act_ont_015.owl");
            manager.saveOntology(ontology, new FunctionalSyntaxDocumentFormat(), new FileOutputStream(fileout));
        } catch (OWLOntologyStorageException | FileNotFoundException | UnknownOWLOntologyException ex) {
            ex.printStackTrace();
        }
    }

    public OWLOntologyManager getManager() {
        return manager;
    }

    public PrefixManager getPrefixManager() {
        return pm;
    }

    public List<Activity> getActivities() {

        // setup and load the ontolgy
        if (ontology == null) {
            Map<String, Object> ontologyPm = ServiceUtils.ontologyAssetsSetup("act_ont_015.owl", manager);
            ontology = (OWLOntology) ontologyPm.get("ontology");
            pm = (PrefixManager) ontologyPm.get("pm");
        }

        if (!ontology.getOntologyID().getVersionIRI().isPresent()) {
            version = 1;
        } else {
            String versionIRI = ontology.getOntologyID().getVersionIRI().get().getNamespace();
            if (versionIRI.contains("absolute:")) {
                versionIRI = versionIRI.substring(versionIRI.indexOf("ute:") + 4);
            }
            version = Integer.parseInt(versionIRI);
            version += 1;
        }

        // create and modify activities
        List<Activity> activities = new ArrayList<>();
        OWLActivity owlActivity = null;
        int i = 0;
        for (OWLAxiom axiom : ontology.axioms().collect(Collectors.toList())) {
            if (axiom instanceof SWRLRule) {
                SWRLRule rule = (SWRLRule) axiom;
                if (rule.bodyList().size() != 0 && rule.headList().size() != 0) {
                    owlActivity = new OWLActivity((SWRLRule) axiom);
                    activities.add(OwlToPojo.getPojoActivity(owlActivity, i));
                    i += 1;
                }
            }
        }
        return activities;
    }

    public void updateActivity(Map<String, Object> activity) {
        String activityName = ((String) activity.get("name"));

        // add the activity into the ontology
        OWLClass activityClass = manager.getOWLDataFactory().getOWLClass(":" +
                activityName,
                pm);
        if (!ontology.containsClassInSignature(activityClass.getIRI())) {
            OWLClass rootActivityClass = manager.getOWLDataFactory().getOWLClass(":" +
                    ActivityList.ROOT_ACTIVITY, pm);
            OWLAxiom subclassAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(activityClass,
                    rootActivityClass);
            ontology.add(subclassAxiom);
        }

        // remove the old swrl rule
        ontology.axioms().forEach(axiom -> {
            if (axiom instanceof SWRLRule) {
                SWRLRule rule = (SWRLRule) axiom;
                SWRLAtom act = rule.head().findFirst().get();
                OWLClass ruleHeadClass = (OWLClass) act.getPredicate();
                if (activityName.equals(ruleHeadClass.getIRI().getShortForm())) {
                    ontology.remove(rule);
                }
            }
        });

        // create and add the new swrl rule
        String bodyString = PojoToOWL.createSwrlRuleBodyString(activity);
        String headString = activityName + "(a)";

        if (bodyString != "Activity(a)") {
            SWRLRule rule = SWRLRuleFactory.getSWRLRuleFromString(bodyString, headString,
                    manager, ontology, pm);
            if (rule != null) {
                ontology.add(rule);
            } else {
                System.err.println("Empty swrl rule is created");
            }
        }

        IRI versionIRI = IRI.create(String.valueOf(version));
        SetOntologyID change = new SetOntologyID(ontology,
                new OWLOntologyID(ontology.getOntologyID().getOntologyIRI(), Optional.of(versionIRI)));
        version += 1;
        ontology.getOWLOntologyManager().applyChange(change);
        // save the ontology
        try {
            File fileout = new File("./act_ont_015.owl");
            manager.saveOntology(ontology, new FunctionalSyntaxDocumentFormat(), new FileOutputStream(fileout));
        } catch (OWLOntologyStorageException | FileNotFoundException | UnknownOWLOntologyException ex) {
            ex.printStackTrace();
        }
    }

    public void addActivity(Map<String, Object> activity) {

        String activityName = ((String) activity.get("name"));

        // add the activity into the ontology
        OWLClass activityClass = manager.getOWLDataFactory().getOWLClass(":" +
                activityName,
                pm);
        OWLClass rootActivityClass = manager.getOWLDataFactory().getOWLClass(":" +
                ActivityList.ROOT_ACTIVITY, pm);
        OWLAxiom subclassAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(activityClass,
                rootActivityClass);
        ontology.add(subclassAxiom);

        // // create the swrl rule
        String bodyString = PojoToOWL.createSwrlRuleBodyString(activity);
        String headString = activityName + "(a)";

        if (bodyString != "Activity(a)") {
            SWRLRule rule = SWRLRuleFactory.getSWRLRuleFromString(bodyString, headString,
                    manager, ontology, pm);
            ontology.add(rule);
        }

        IRI versionIRI = IRI.create(String.valueOf(version));
        SetOntologyID change = new SetOntologyID(ontology,
                new OWLOntologyID(ontology.getOntologyID().getOntologyIRI(), Optional.of(versionIRI)));
        version += 1;
        ontology.getOWLOntologyManager().applyChange(change);
        // save the ontology
        try {
            File fileout = new File("./act_ont_015.owl");
            manager.saveOntology(ontology, new FunctionalSyntaxDocumentFormat(), new FileOutputStream(fileout));
        } catch (OWLOntologyStorageException | FileNotFoundException | UnknownOWLOntologyException ex) {
            ex.printStackTrace();
        }
    }

    public void removeActivity(String activity) {
        // remove the corresponding axioms and rules
        OWLClass activityClass = manager.getOWLDataFactory().getOWLClass(":" + activity,
                pm);
        OWLDeclarationAxiom declAxiom = manager.getOWLDataFactory().getOWLDeclarationAxiom(activityClass);
        OWLClass rootActivityClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.ROOT_ACTIVITY, pm);
        OWLAxiom subclassAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(activityClass, rootActivityClass);
        ontology.removeAxiom(subclassAxiom);
        ontology.removeAxiom(declAxiom);

        List<OWLAxiom> toRemove = new ArrayList<>();
        ontology.axioms().forEach(axiom -> {
            if (axiom.getClassesInSignature().contains(activityClass)) {
                toRemove.add(axiom);
            }
        });

        for (OWLAxiom axiom : toRemove) {
            ontology.removeAxiom(axiom);
        }

        ontology.axioms().forEach(axiom -> {
            System.out.println(axiom.getClassesInSignature());
        });

        ontology.axioms().forEach(axiom -> {
            if (axiom instanceof SWRLRule) {
                SWRLRule rule = (SWRLRule) axiom;
                SWRLAtom act = rule.head().findFirst().get();
                OWLClass ruleHeadClass = (OWLClass) act.getPredicate();
                if (activity.equals(ruleHeadClass.getIRI().getShortForm())) {
                    ontology.remove(rule);
                }
            }
        });

        IRI versionIRI = IRI.create(String.valueOf(version));
        SetOntologyID change = new SetOntologyID(ontology,
                new OWLOntologyID(ontology.getOntologyID().getOntologyIRI(), Optional.of(versionIRI)));
        version += 1;
        ontology.getOWLOntologyManager().applyChange(change);
        // save the ontology
        try {
            File fileout = new File("./act_ont_015.owl");
            manager.saveOntology(ontology, new FunctionalSyntaxDocumentFormat(), new FileOutputStream(fileout));
        } catch (OWLOntologyStorageException | FileNotFoundException | UnknownOWLOntologyException ex) {
            ex.printStackTrace();
        }
    }
}

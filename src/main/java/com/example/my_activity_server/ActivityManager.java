package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class ActivityManager {

    public OWLOntology addActivity(PrefixManager pm, String activityName, Map<String, String> axiomEntities,
            OWLOntologyManager manager, OWLOntology ontology, OWLReasoner reasoner) {

        // create OWLCLass reference
        OWLClass activityClass = manager.getOWLDataFactory().getOWLClass(":" + activityName, pm);
        // create the list of OWL expressions
        ontology = createClassExpressioins(axiomEntities, pm, activityClass, manager, ontology, reasoner);

        return ontology;
    }

    private OWLOntology createClassExpressioins(Map<String, String> axiomEntities, PrefixManager pm,
            OWLClass activityClass, OWLOntologyManager manager, OWLOntology ontology, OWLReasoner reasoner) {

        OWLDataFactory df = manager.getOWLDataFactory();
        Set<OWLClassExpression> expressions = new HashSet<OWLClassExpression>();

        // add the new activity to the activity set (activityClass -> Activity)
        System.out.println(IRI.create(pm.toString()));
        OWLClass rootActivityClass = manager.getOWLDataFactory().getOWLClass(":Activity", pm);

        manager.addAxiom(ontology, df.getOWLSubClassOfAxiom(activityClass, rootActivityClass));
        ontology.logicalAxioms().forEach(s -> {
            System.out.println(s);
        });

        axiomEntities.forEach((entitiy, entityValue) -> {
            OWLObjectProperty objectProperty = df.getOWLObjectProperty(":" + entitiy, pm);
            OWLClass object = df.getOWLClass(":" + entityValue, pm);
            OWLClassExpression objectPropertyExpression = df.getOWLObjectSomeValuesFrom(objectProperty, object);
            expressions.add(objectPropertyExpression);
        });

        OWLClassExpression activityExpression = df.getOWLObjectIntersectionOf(expressions);

        OWLAxiom axiom = df.getOWLEquivalentClassesAxiom(activityClass, activityExpression);
        manager.addAxiom(ontology, axiom);

        ontology.logicalAxioms().forEach(s -> {
            System.out.println(s);
        });
        return ontology;
    }

    public void addActivityInstance(Map<String, String> axiomEntities, PrefixManager pm, String activityInstanceName,
            OWLOntologyManager manager) {
        OWLDataFactory df = manager.getOWLDataFactory();
        OWLNamedIndividual activityInstance = df.getOWLNamedIndividual(activityInstanceName, pm);

        axiomEntities.forEach((entitiy, entityValue) -> {

        });

    }

    public static List<OWLCLass> getActivityClasses(OWLOntology ontology, OWLOntologyManager manager) {
        List<OWLCLass> activityClasses = new ArrayList<>();
        ontology.axioms().forEach(axiom -> {
            if (axiom instanceof SWRLRule) {
                SWRLRule rule = (SWRLRule) axiom;
                OWLClass activityClass = (OWLClass) rule.headList().get(0).getPredicate();
                rule.headList().get(0).getPredicate();
                activityClasses.add((OWLCLass) activityClass);
            }
        });

        return activityClasses;
    }

}

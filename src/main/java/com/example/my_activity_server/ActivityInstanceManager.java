package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.my_activity_server.model.ActivityInstance;
import com.example.my_activity_server.model.EventInstance;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;

public class ActivityInstanceManager {

    public static List<EventInstance> getEvents(Set<OWLIndividualAxiom> axioms, OWLOntology ontology) {

        List<EventInstance> events = new ArrayList<>();

        axioms.forEach(axiom -> {
            if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom objPropAxiom = (OWLObjectPropertyAssertionAxiom) axiom;
                // hasEvent property
                String propName = objPropAxiom.getProperty().getNamedProperty().getIRI().getShortForm();
                if (propName.equals(Predicate.HAS_EVENT)) {
                    OWLNamedIndividual eventIndividual = objPropAxiom.getObject().asOWLNamedIndividual(); // tooth_brush_01
                    EventInstance event = new EventInstance(); // set the name
                    Set<OWLIndividualAxiom> eventIndAxioms = ontology.getAxioms(eventIndividual);
                    eventIndAxioms.forEach(axiom2 -> {
                        if (axiom2 instanceof OWLDataPropertyAssertionAxiom) { // get start and end times
                            OWLDataPropertyAssertionAxiom dataPropAxiom = (OWLDataPropertyAssertionAxiom) axiom2;
                            OWLDataProperty dataProp = dataPropAxiom.getProperty().asOWLDataProperty();
                            String dataPropName = dataProp.getIRI().getShortForm();
                            String time = dataPropAxiom.getObject().getLiteral();
                            if (dataPropName.equals(Predicate.HAS_START_TIME)) {
                                event.setStartTime(Integer.parseInt(time));
                            } else if (dataPropName.equals(Predicate.HAS_END_TIME)) {
                                event.setEndTime(Integer.parseInt(time));
                            }
                        } else if (axiom2 instanceof OWLClassAssertionAxiom) {
                            OWLClassAssertionAxiom classAxiom = (OWLClassAssertionAxiom) axiom2;
                            // event type (e.g., ToothBrush)
                            event.setType(classAxiom.getClassExpression().asOWLClass().getIRI().getShortForm());
                            event.setName(eventIndividual.getIRI().getShortForm());
                        }
                    });
                    events.add(event);
                }
            }
        });

        return events;
    }

    public static OWLOntology addActivityInstances(List<ActivityInstance> instances, OWLOntologyManager manager,
            OWLOntology ontology,
            PrefixManager pm) {

        OWLDataFactory df = manager.getOWLDataFactory();

        // The root activity class
        OWLClass rootActivityClass = df.getOWLClass(":" + ActivityList.ROOT_ACTIVITY, pm);
        for (ActivityInstance instance : instances) {
            // create the activity individual as an activity
            OWLNamedIndividual activityInd = df.getOWLNamedIndividual(instance.getName(), pm);

            // check if already exist
            // for (OWLNamedIndividual ind : ontology.getIndividualsInSignature()) {
            // if (ind.getIRI().getShortForm().equals(instance.getName())) {
            // System.out.println("The individual '" + instance.getName() + "' already
            // exist");
            // continue;
            // }
            // }

            OWLDeclarationAxiom activityIndDecAxiom = df.getOWLDeclarationAxiom(activityInd);
            ontology.add(activityIndDecAxiom);
            OWLClassAssertionAxiom activityClassAxiom = df.getOWLClassAssertionAxiom(rootActivityClass, activityInd);
            ontology.add(activityClassAxiom);
            // create events, object properties, and data properties
            for (EventInstance event : instance.getEvents()) {
                OWLNamedIndividual eventInd = df.getOWLNamedIndividual(
                        instance.getName() + "_" + event.getType() + "_" + event.getEndTime(), pm);
                OWLDeclarationAxiom eventIndDecAxiom = df.getOWLDeclarationAxiom(eventInd);
                ontology.add(eventIndDecAxiom);
                // event class and subclass of Objects
                OWLClass eventClass = df.getOWLClass(convertToPascalCase(event.getType()), pm);
                OWLClassAssertionAxiom eventClassAxiom = df.getOWLClassAssertionAxiom(eventClass, eventInd);
                ontology.add(eventClassAxiom);
                OWLClass objectsClass = df.getOWLClass("Objects", pm);
                OWLSubClassOfAxiom eventSubClassAxiom = df.getOWLSubClassOfAxiom(eventClass, objectsClass);
                ontology.add(eventSubClassAxiom);
                // object property (hasEvent)
                OWLObjectProperty objProperty = df.getOWLObjectProperty(":" + Predicate.HAS_EVENT, pm);
                OWLObjectPropertyAssertionAxiom objPropertyAxiom = df.getOWLObjectPropertyAssertionAxiom(objProperty,
                        activityInd, eventInd);
                ontology.add(objPropertyAxiom);
                // data properties (hasStartTime, hasEndTime)
                OWLDataProperty dataPropertyStartTime = df.getOWLDataProperty(":" + Predicate.HAS_START_TIME, pm);
                OWLDataProperty dataPropertyEndTime = df.getOWLDataProperty(":" + Predicate.HAS_END_TIME, pm);
                OWLDataPropertyAssertionAxiom dataPropertyStartTimeAxiom = df
                        .getOWLDataPropertyAssertionAxiom(dataPropertyStartTime, eventInd, (int) event.getStartTime());
                ontology.add(dataPropertyStartTimeAxiom);
                OWLDataPropertyAssertionAxiom dataPropertyEndTimeAxiom = df
                        .getOWLDataPropertyAssertionAxiom(dataPropertyEndTime, eventInd, (int) event.getEndTime());
                ontology.add(dataPropertyEndTimeAxiom);
            }

        }

        return ontology;
    }

    private static String convertToPascalCase(String str) {
        String newStr = "";
        for (String strPart : str.split("_")) {
            newStr += strPart.substring(0, 1).toUpperCase() + strPart.substring(1);
        }

        return newStr;
    }
}

// Declaration of NamedIndividuals (the activity and its events)
// ClassAssertion for the activity
// ClassAssertion for the events
// ObjectPropertyAssertion for connecting events into the activity
// DataPropertyAssertion for events (startTime and endTime)
// DisjointClasses ??
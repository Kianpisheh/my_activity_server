package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.my_activity_server.model.EventInstance;

import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

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
}
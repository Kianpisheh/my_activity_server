package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLBuiltInAtom;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLBuiltInAtomImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLClassAtomImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLDataPropertyAtomImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLObjectPropertyAtomImpl;

public class Axiom {
    public static final int INTERACTION = 0;
    public static final int INTERACTION_DURATION_LESS = 1;
    public static final int INTERACTION_DURATION_MORE = 2;
    public static final int INTERACTION_DURATION_LESS_MORE = 3;
    public static final int INTERACTION_ORDER = 4;
    public static final int INTERACTION_TIME_DISTANCE_LESS = 5;
    public static final int INTERACTION_TIME_DISTANCE_MORE = 6;
    public static final int INTERACTION_TIME_DISTANCE_LESS_MORE = 7;
    // }

    public Axiom() {

    }

    public static List<ActionEvent> getEvents(List<SWRLAtom> atoms) {
        List<ActionEvent> events = new ArrayList<>();

        atoms.forEach(atom -> {
            if (atom instanceof SWRLObjectPropertyAtomImpl) {
                OWLObjectPropertyImpl predicateClass = (OWLObjectPropertyImpl) atom.getPredicate();
                String PredicateName = predicateClass.getIRI().getShortForm();
                if (PredicateName.equals(Predicate.HAS_EVENT)) {
                    ActionEvent event = new ActionEvent();
                    SWRLArgument eventArgument = (SWRLArgument) atom.allArguments().toArray()[1];
                    atoms.forEach(atom2 -> {
                        SWRLArgument atom2Arg = atom2.allArguments().findFirst().get();
                        // the ones that the first variable is "e"
                        if (atom2Arg.equals(eventArgument)) {
                            if (atom2 instanceof SWRLClassAtomImpl) { // event predicate
                                event.setPredicateAtom(atom2);
                                OWLClassImpl eventPredicate = (OWLClassImpl) atom2.getPredicate();
                                String predicateName = eventPredicate.getIRI().getShortForm();
                                event.setType(predicateName);
                                IRI atom2ArgIRI = (IRI) atom2Arg.components().findFirst().get();
                                event.setVarName(atom2ArgIRI.getRemainder().get());
                                event.setDependencies(atoms, atom2ArgIRI.getRemainder().get());
                            } else if (atom2 instanceof SWRLDataPropertyAtomImpl) { // start end end times
                                OWLDataProperty dataProp = (OWLDataProperty) atom2.getPredicate();
                                String propName = dataProp.getIRI().getRemainder().get();
                                if (propName.equals(Predicate.HAS_START_TIME)) {
                                    event.setStartTimeAtom(atom2);
                                } else if (propName.equals(Predicate.HAS_END_TIME)) {
                                    event.setEndTimeAtom(atom2);
                                }
                            }
                        }
                    });
                    if (!events.contains(event) && (!event.isEmpty()))
                        events.add(event);
                }
            }
        });

        return events;
    }

    public static EventConstraint getAxiomConstraint(List<SWRLAtom> atoms, List<ActionEvent> events) {
        SWRLBuiltInAtomImpl lessThanAtom = null;
        SWRLBuiltInAtomImpl greaterThanAtom = null;
        SWRLBuiltInAtomImpl operatorAtom = null;

        String operator = "";
        String arithmeticOperator = "";
        List<String> operatorArgs = new ArrayList<>();
        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLBuiltInAtom) {
                IRI atomIRI = (IRI) atom.getPredicate();
                operator = atomIRI.getShortForm();
                if (operator.equals("subtract")) {
                    for (SWRLArgument arg : atom.allArguments().collect(Collectors.toList())) {
                        IRI iri = (IRI) arg.components().findFirst().get();
                        operatorArgs.add(iri.getRemainder().get());
                    }
                    operatorAtom = (SWRLBuiltInAtomImpl) atom;
                    arithmeticOperator = operator;
                } else if (operator.equals("greaterThan")) {
                    greaterThanAtom = (SWRLBuiltInAtomImpl) atom;
                } else if (operator.equals("lessThan")) {
                    lessThanAtom = (SWRLBuiltInAtomImpl) atom;
                }
            }
        }

        EventConstraint constraint = new EventConstraint(operatorArgs, arithmeticOperator);
        constraint.setOperatorAtom(operatorAtom);
        constraint.setGreaterThanAtom(greaterThanAtom);
        constraint.setLessThanAtom(lessThanAtom);
        constraint.setEvents(events);

        if ((greaterThanAtom == null) && (lessThanAtom == null))
            return null;

        return constraint;
    }

    public static int getType(int numEvents, SWRLBuiltInAtom greaterThanAtom, SWRLBuiltInAtom lessThanAtom) {
        int type = -1;

        if (greaterThanAtom == null && lessThanAtom == null)
            type = INTERACTION;
        else if (greaterThanAtom == null || lessThanAtom == null) {
            if (lessThanAtom != null & numEvents == 1)
                type = INTERACTION_DURATION_MORE;
            else if (lessThanAtom != null && (numEvents == 2))
                type = INTERACTION_TIME_DISTANCE_MORE;
            else if (greaterThanAtom != null & (numEvents == 1))
                type = INTERACTION_DURATION_LESS;
            else if (greaterThanAtom != null & (numEvents == 2))
                type = INTERACTION_TIME_DISTANCE_LESS;
        }

        else if (greaterThanAtom != null || lessThanAtom != null) {
            if (numEvents == 1)
                type = INTERACTION_DURATION_LESS_MORE;
            else if (numEvents == 2)
                type = INTERACTION_TIME_DISTANCE_LESS_MORE;
        }

        if (type == -1) {
            System.err.println("proposition type not recognized.");
        }

        return type;
    }
}

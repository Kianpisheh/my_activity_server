package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;

// 
public class UserExplainer {

    public static void explain(OWLNamedIndividual ind, String activity, String type,
            OWLOntologyManager manager, OWLOntology ontology, PrefixManager pm) {

        SWRLRule rule = null;
        if (type.equals("why")) {
            final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
            final PelletExplanation explainer = new PelletExplanation(reasoner);

            // get the target activity class in the ontology
            OWLClass targetActivityClass = manager.getOWLDataFactory().getOWLClass(":" + activity, pm);
            // get satisfied axioms
            Set<OWLAxiom> axioms = explainer.getInstanceExplanation(ind, targetActivityClass);
            // let's find the satisfied swrl rule

            for (OWLAxiom axiom : axioms) {
                if (axiom instanceof SWRLRule) {
                    rule = (SWRLRule) axiom;
                }
            }
            if (rule == null) {
                System.out.println("The sample is not an instance of " + type + " activity");
                return;
            }

            // get the rule type, events and constraints
            List<SWRLAtom> axiomAtoms = rule.bodyList();
            int axiomType = getAxiomType(rule.bodyList());
            List<ActionEvent> axiomEvents = Axiom.getEvents(axiomAtoms);
            EventConstraint axiomConstraint = Axiom.getAxiomConstraint(axiomAtoms, axiomEvents);

            String explanation = getExplanation(axiomType, axiomEvents, axiomConstraint, type);
            System.out.println(explanation);
        } else if (type.equals("why not")) {
            List<ActionEvent> unstatisfiedEvents = new ArrayList<>();
            List<EventConstraint> unstatisfiedConstraint = new ArrayList<>();
            // find the swrl rule for the target activity
            for (OWLAxiom axiom : ontology.axioms().collect(Collectors.toList())) {
                if (axiom instanceof SWRLRule) {
                    rule = (SWRLRule) axiom;
                    if (rule.headList().get(0).toString().contains(activity)) {
                        // find the unstatisfied events
                        List<ActionEvent> events = Axiom.getEvents(rule.bodyList());
                        for (ActionEvent event : events) {
                            boolean eSatisfied = event.isSatisfied(ind, manager, ontology, pm);
                            if (!eSatisfied) {
                                unstatisfiedEvents.add(event);
                            }
                        }
                        // find the unsatisfied constraints
                        EventConstraint constraint = Axiom.getAxiomConstraint(rule.bodyList(), events);
                        if (constraint == null)
                            continue;
                        boolean cSatisfied = constraint.isSatisfied(ind, manager, ontology, pm);
                        if (!cSatisfied) {
                            unstatisfiedConstraint.add(constraint);
                        }
                    }
                }
            }

            // TODO: unsatisfiedConstraints List vs type
            int axiomType = getAxiomType(rule.bodyList());
            String explanation = getExplanation(axiomType, unstatisfiedEvents, unstatisfiedConstraint.get(0), type);
            System.out.println(explanation);
        }
    }

    private static int getAxiomType(List<SWRLAtom> atoms) {
        List<ActionEvent> events = Axiom.getEvents(atoms);
        EventConstraint axiomConstraint = Axiom.getAxiomConstraint(atoms, events);
        int type = Axiom.getType(events.size(), axiomConstraint.getGreaterThanAtom(),
                axiomConstraint.getLessThanAtom());

        return type;
    }

    private static String getExplanation(int axiomType, List<ActionEvent> axiomEvents,
            EventConstraint axiomConstraint, String type) {

        String explanation = "";
        if (type.equals("why")) {
            explanation = explainWhy(axiomType, axiomEvents, axiomConstraint, type);
        } else if (type.equals("why not")) {
            explanation = explainWhyNot(axiomType, axiomEvents, axiomConstraint, type);
        }
        return explanation;
    }

    private static String explainWhy(int axiomType, List<ActionEvent> axiomEvents,
            EventConstraint axiomConstraint, String type) {

        if (axiomEvents.size() == 0)
            return "";

        String explanation = "You have interacted with ";
        switch (axiomType) {
            case Axiom.INTERACTION: {
                explanation = String.format(explanation, axiomEvents.get(0).getType());
                break;
            }
            case Axiom.INTERACTION_DURATION_LESS: {
                explanation = String.format(explanation + "%s less than %s seconds.", axiomEvents.get(0).getType(),
                        axiomConstraint.getMaxValue());
                break;
            }
            case Axiom.INTERACTION_DURATION_MORE: {
                explanation = String.format(explanation + "%s more than %s seconds.", axiomEvents.get(0).getType(),
                        axiomConstraint.getMinValue());
                break;
            }
            case Axiom.INTERACTION_DURATION_LESS_MORE: {
                explanation = String.format(explanation + "%s more than %s seconds and less than %s seconds.",
                        axiomEvents.get(0).getType(),
                        axiomConstraint.getMinValue(),
                        axiomConstraint.getMaxValue());
                break;
            }
            case Axiom.INTERACTION_TIME_DISTANCE_LESS: {
                explanation = String.format(explanation + " both %s and %s in less than %s seconds.",
                        axiomEvents.get(0).getType(),
                        axiomEvents.get(1).getType(),
                        axiomConstraint.getMaxValue());
                break;
            }
            case Axiom.INTERACTION_TIME_DISTANCE_MORE: {
                explanation = String.format(explanation + " both %s and %s in more than %s seconds.",
                        axiomEvents.get(0).getType(),
                        axiomEvents.get(1).getType(),
                        axiomConstraint.getMinValue());
                break;
            }
            case Axiom.INTERACTION_TIME_DISTANCE_LESS_MORE: {
                explanation = String.format(
                        explanation + " both %s and %s in more than %s seconds and less than %s seconds.",
                        axiomEvents.get(0).getType(),
                        axiomEvents.get(1).getType(),
                        axiomConstraint.getMinValue(),
                        axiomConstraint.getMaxValue());
                break;
            }
            case Axiom.INTERACTION_ORDER: {
                explanation = String.format(explanation + " with %s after using %s.",
                        axiomEvents.get(0).getType(),
                        axiomEvents.get(1).getType());
                break;
            }
            default:
                break;
        }

        return explanation;
    }

    private static String explainWhyNot(int axiomType, List<ActionEvent> axiomEvents,
            EventConstraint axiomConstraint, String type) {

        return "";
    }
}

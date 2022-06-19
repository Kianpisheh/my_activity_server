package com.example.my_activity_server.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.my_activity_server.ActivityList;
import com.example.my_activity_server.Predicate;
import com.example.my_activity_server.SWRLRuleFactory;
import com.example.my_activity_server.API.ActionEventConstraintPojo;
import com.example.my_activity_server.API.OwlToPojo;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;

@Service
public class ExplainerService {

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology;
    PrefixManager pm = null;
    String ontIRI = "";

    @Autowired
    private ActivityService activityService;

    public Map<String, Object> why(Map<String, String> query) {

        // setup and load the ontolgy
        ontology = activityService.getOntology();
        manager = activityService.getManager();
        pm = activityService.getPrefixManager();
        if (ontology == null) {
            Map<String, Object> ontologyPm = ServiceUtils.ontologyAssetsSetup("act_ont_015.owl", manager);
            ontology = (OWLOntology) ontologyPm.get("ontology");
            pm = (PrefixManager) ontologyPm.get("pm");
        }

        // reasoner setup
        PelletExplanation.setup();
        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        final PelletExplanation explainer = new PelletExplanation(reasoner);

        // retrieve the query activity instance and the target activity
        OWLClass targetActivityClass = manager.getOWLDataFactory().getOWLClass(":" + query.get("activity"), pm);
        OWLNamedIndividual ind = manager.getOWLDataFactory()
                .getOWLNamedIndividual(query.get("instance"), pm);

        // let's find the satisfied swrl rule
        Set<OWLAxiom> satisfiedAxioms = explainer.getInstanceExplanation(ind, targetActivityClass);

        SWRLRule rule = null;
        for (OWLAxiom axiom : satisfiedAxioms) {
            if (axiom instanceof SWRLRule) {
                rule = (SWRLRule) axiom;
            }
        }
        if (rule == null) {
            System.out.println("The sample is not an instance of " + query.get("activity") + " activity");
            return null;
        }

        // get the rule type, and the entailed events and constraints
        List<SWRLAtom> axiomAtoms = rule.bodyList();

        // Entailed axioms (i.e., events and constraints)
        List<String> entailedEvents = OwlToPojo.getEvents(axiomAtoms);
        List<ActionEventConstraintPojo> entailedConstraints = OwlToPojo.getConstraints(axiomAtoms);

        Map<String, Object> entailedPropositions = new HashMap<>();
        entailedPropositions.put("events", entailedEvents);
        entailedPropositions.put("constraints", entailedConstraints);

        // find event individuals (axioms) that are responsible for the activity
        // entailment
        List<String> entailedEventIndividuals = new ArrayList<>();
        for (OWLAxiom axiom : satisfiedAxioms) {
            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom axiom2 = (OWLClassAssertionAxiom) axiom;
                String eventName = axiom2.getClassExpression().asOWLClass().getIRI().getShortForm();
                if (entailedEvents.contains(eventName)) {
                    String eventIndividualName = axiom2.getIndividual().asOWLNamedIndividual().getIRI().getShortForm();
                    entailedEventIndividuals.add(eventIndividualName);
                }
            }
        }

        // find start and end times of entailed individual events
        List<Double> startTimes = new ArrayList<>();
        List<Double> endTimes = new ArrayList<>();
        for (String entailedIndividual : entailedEventIndividuals) {
            for (OWLAxiom axiom : ontology.getAxioms()) {
                if (axiom instanceof OWLDataPropertyAssertionAxiom) {
                    OWLDataPropertyAssertionAxiom axiom2 = (OWLDataPropertyAssertionAxiom) axiom;
                    String propName = axiom2.getProperty().asOWLDataProperty().getIRI().getShortForm();
                    if (propName.equals(Predicate.HAS_START_TIME)) {
                        String eventInstanceName = axiom2.getSubject().asOWLNamedIndividual().getIRI().getShortForm();
                        if (entailedIndividual.equals(eventInstanceName)) {
                            startTimes.add(axiom2.getObject().asLiteral().get().parseDouble());
                        }
                    } else if (propName.equals(Predicate.HAS_END_TIME)) {
                        String eventInstanceName = axiom2.getSubject().asOWLNamedIndividual().getIRI().getShortForm();
                        if (entailedIndividual.equals(eventInstanceName)) {
                            endTimes.add(axiom2.getObject().asLiteral().get().parseDouble());
                        }
                    }
                }
            }
        }

        entailedPropositions.put("individuals", entailedEventIndividuals);
        entailedPropositions.put("startTimes", startTimes);
        entailedPropositions.put("endTimes", endTimes);
        entailedPropositions.put("type", "why");

        return entailedPropositions;
    }

    public Map<String, Object> whyNot(Map<String, String> query) {

        PelletExplanation.setup();
        SWRLRule rule = null;
        List<String> unstatisfiedEvents = new ArrayList<>();
        List<ActionEventConstraintPojo> unstatisfiedConstraint = new ArrayList<>();

        // setup and load the ontolgy
        ontology = activityService.getOntology();
        manager = activityService.getManager();
        pm = activityService.getPrefixManager();
        if (ontology == null) {
            Map<String, Object> ontologyPm = ServiceUtils.ontologyAssetsSetup("act_ont_015.owl", manager);
            ontology = (OWLOntology) ontologyPm.get("ontology");
            pm = (PrefixManager) ontologyPm.get("pm");
        }

        // get the queried individual
        OWLNamedIndividual activityIndividual = null;
        Set<OWLNamedIndividual> inds = ontology.getIndividualsInSignature();
        for (OWLNamedIndividual ind : inds) {
            if (ind.getIRI().getShortForm().equals(query.get("instance"))) {
                activityIndividual = ind;
                break;
            }
        }

        // find the swrl rule for the target activity
        for (OWLAxiom axiom : ontology.axioms().collect(Collectors.toList())) {
            if (axiom instanceof SWRLRule) {
                rule = (SWRLRule) axiom;
                if (rule.headList().get(0).toString().contains(query.get("activity"))) {
                    // find the unstatisfied events
                    List<String> events = OwlToPojo.getEvents(rule.bodyList());
                    for (String event : events) {
                        boolean eSatisfied = this.isEventSatisfied(event, activityIndividual, manager, ontology, pm);
                        if (!eSatisfied) {
                            unstatisfiedEvents.add(event);
                        }
                    }
                    // find the unsatisfied constraints
                    List<ActionEventConstraintPojo> constraints = OwlToPojo.getConstraints(rule.bodyList());
                    for (ActionEventConstraintPojo constraint : constraints) {
                        boolean cSatisfied = this.isContraintSatisfied(constraint, activityIndividual, manager,
                                ontology, pm);
                        if (!cSatisfied) {
                            unstatisfiedConstraint.add(constraint);
                        }
                    }
                }
            }
        }

        Map<String, Object> unsatisfiedPropositions = new HashMap<>();
        unsatisfiedPropositions.putIfAbsent("events", unstatisfiedEvents);
        unsatisfiedPropositions.putIfAbsent("constraints", unstatisfiedConstraint);
        unsatisfiedPropositions.putIfAbsent("type", "why_not");

        return unsatisfiedPropositions;
    }

    // private methods
    private boolean isEventSatisfied(String eventType, OWLNamedIndividual ind, OWLOntologyManager manager,
            OWLOntology ontology,
            PrefixManager pm) {

        // add the single event activity into the ontology
        OWLClass singleEventClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.SINGLE_EVENT_ACTIVITY,
                pm);
        OWLClass rootActivityClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.ROOT_ACTIVITY, pm);
        OWLAxiom subclassAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(singleEventClass, rootActivityClass);
        ontology.add(subclassAxiom);

        // SWRL rule
        String bodyString = "Activity(a)^hasEvent(a,e)^" + eventType + "(e)";
        String headString = ActivityList.SINGLE_EVENT_ACTIVITY + "(a)";
        SWRLRule rule = SWRLRuleFactory.getSWRLRuleFromString(bodyString, headString, manager, ontology, pm);
        ontology.add(rule);

        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        final PelletExplanation explainer = new PelletExplanation(reasoner);

        Set<OWLAxiom> satisfiedAtoms = explainer.getInstanceExplanation(ind,
                singleEventClass);

        // for (OWLAxiom axiom : satisfiedAtoms) {
        // if (axiom instanceof OWLClassAssertionAxiom) {
        // OWLClassAssertionAxiom axiom2 = (OWLClassAssertionAxiom) axiom;
        // if (axiom2.getIndividual().equals(ind)) {

        // }
        // }
        // }

        ontology.remove(rule);
        ontology.remove(subclassAxiom);
        return (satisfiedAtoms.size() > 0);
    }

    private boolean isContraintSatisfied(ActionEventConstraintPojo constraint, OWLNamedIndividual ind,
            OWLOntologyManager manager, OWLOntology ontology,
            PrefixManager pm) {
        // add the single event activity into the ontology
        OWLClass singleEventClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.SINGLE_EVENT_ACTIVITY,
                pm);
        OWLClass rootActivityClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.ROOT_ACTIVITY, pm);
        OWLAxiom subclassAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(singleEventClass, rootActivityClass);
        ontology.add(subclassAxiom);

        String headString = ActivityList.SINGLE_EVENT_ACTIVITY + "(a)";
        String bodyString = "Activity(a)";
        int i = 0;
        for (String event : constraint.getEvents()) {
            // event predicate
            bodyString += String.format("^%s(a,e%s)^%s(e%s)", Predicate.HAS_EVENT, i, event, i);
            // start and end times
            if (constraint.getTh1() >= 0) {
                bodyString += String.format("^%s(e%s,t1_e%s)", Predicate.HAS_START_TIME, i, i);
            }
            if (constraint.getTh2() >= 0) {
                bodyString += String.format("^%s(e%s,t2_e%s)", Predicate.HAS_END_TIME, i, i);
            }
        }

        // built-in operator
        String t2 = "";
        String t1 = "";
        String dt = "dt";
        if (constraint.getEvents().size() == 1) {
            t2 = "t2_e0";
            t1 = "t1_e0";
        } else if (constraint.getEvents().size() == 2) {
            t2 = "t1_e1";
            t1 = "t2_e0";
        }
        bodyString += "^" + "subtract" + "(" + "dt," + t2 + "," + t1 + ")";

        // restrections
        if (constraint.getTh1() >= 0) {
            bodyString += String.format("^greaterThan(%s,%s)", dt, constraint.getTh1());
        }
        if (constraint.getTh2() >= 0) {
            bodyString += String.format("^lessThan(%s,%s)", dt, constraint.getTh2());
        }

        SWRLRule rule = SWRLRuleFactory.getSWRLRuleFromString(bodyString, headString, manager, ontology, pm);
        ontology.add(rule);

        // check the rule entailment //TODO: internal reasoner might not be a good ideal
        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        final PelletExplanation explainer = new PelletExplanation(reasoner);
        Set<OWLAxiom> satisfiedAtoms = explainer.getInstanceExplanation(ind, singleEventClass);

        ontology.remove(subclassAxiom);
        ontology.remove(rule);

        return (satisfiedAtoms.size() > 0);

    }
}

// private static String explainWhy(int axiomType, List<ActionEvent>
// axiomEvents,
// EventConstraint axiomConstraint, String type) {

// if (axiomEvents.size() == 0)
// return "";

// String explanation = "You have interacted with ";
// switch (axiomType) {
// case Axiom.INTERACTION: {
// explanation = String.format(explanation, axiomEvents.get(0).getType());
// break;
// }
// case Axiom.INTERACTION_DURATION_LESS: {
// explanation = String.format(explanation + "%s less than %s seconds.",
// axiomEvents.get(0).getType(),
// axiomConstraint.getMaxValue());
// break;
// }
// case Axiom.INTERACTION_DURATION_MORE: {
// explanation = String.format(explanation + "%s more than %s seconds.",
// axiomEvents.get(0).getType(),
// axiomConstraint.getMinValue());
// break;
// }
// case Axiom.INTERACTION_DURATION_LESS_MORE: {
// explanation = String.format(explanation + "%s more than %s seconds and less
// than %s seconds.",
// axiomEvents.get(0).getType(),
// axiomConstraint.getMinValue(),
// axiomConstraint.getMaxValue());
// break;
// }
// case Axiom.INTERACTION_TIME_DISTANCE_LESS: {
// explanation = String.format(explanation + " both %s and %s in less than %s
// seconds.",
// axiomEvents.get(0).getType(),
// axiomEvents.get(1).getType(),
// axiomConstraint.getMaxValue());
// break;
// }
// case Axiom.INTERACTION_TIME_DISTANCE_MORE: {
// explanation = String.format(explanation + " both %s and %s in more than %s
// seconds.",
// axiomEvents.get(0).getType(),
// axiomEvents.get(1).getType(),
// axiomConstraint.getMinValue());
// break;
// }
// case Axiom.INTERACTION_TIME_DISTANCE_LESS_MORE: {
// explanation = String.format(
// explanation + " both %s and %s in more than %s seconds and less than %s
// seconds.",
// axiomEvents.get(0).getType(),
// axiomEvents.get(1).getType(),
// axiomConstraint.getMinValue(),
// axiomConstraint.getMaxValue());
// break;
// }
// case Axiom.INTERACTION_ORDER: {
// explanation = String.format(explanation + " with %s after using %s.",
// axiomEvents.get(0).getType(),
// axiomEvents.get(1).getType());
// break;
// }
// default:
// break;
// }

// return explanation;
// }

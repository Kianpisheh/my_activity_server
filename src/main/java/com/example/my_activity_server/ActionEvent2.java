package com.example.my_activity_server;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;

public class ActionEvent2 {
    SWRLAtom predicateAtom = null;
    SWRLAtom startTimeAtom = null;
    SWRLAtom endTimeAtom = null;

    public ActionEvent2() {

    }

    public ActionEvent2(SWRLAtom predicateAtom, SWRLAtom startTimeAtom, SWRLAtom endTimeAtom) {
        this.predicateAtom = predicateAtom;
        this.startTimeAtom = startTimeAtom;
        this.endTimeAtom = endTimeAtom;
    }

    public SWRLAtom getPredicateAtom() {
        return predicateAtom;
    }

    public SWRLAtom getStartTimeAtom() {
        return startTimeAtom;
    }

    public SWRLAtom getEndTimeAtom() {
        return endTimeAtom;
    }

    public String getStartTimeVar() {
        SWRLArgument arg = (SWRLArgument) startTimeAtom.allArguments().toArray()[1];
        IRI argIRI = (IRI) arg.components().findFirst().get();
        return argIRI.getShortForm();
    }

    public String getEndTimeVar() {
        SWRLArgument arg = (SWRLArgument) endTimeAtom.allArguments().toArray()[1];
        IRI argIRI = (IRI) arg.components().findFirst().get();
        return argIRI.getShortForm();
    }

    public boolean isEmpty() {
        return (predicateAtom == null);
    }

    public String getType() {
        IRI predicateClass = (IRI) predicateAtom.getPredicate();
        return predicateClass.getShortForm();
    }

    public boolean isSatisfied(OWLNamedIndividual ind, OWLOntologyManager manager, OWLOntology ontology,
            PrefixManager pm) {

        // add the single event activity into the ontology
        OWLClass singleEventClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.SINGLE_EVENT_ACTIVITY,
                pm);
        OWLClass rootActivityClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.ROOT_ACTIVITY, pm);
        OWLAxiom subclassAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(singleEventClass, rootActivityClass);
        ontology.add(subclassAxiom);

        // SWRL rule
        String bodyString = "Activity(a)^hasEvent(a,e)^" + getType() + "(e)";
        String headString = ActivityList.SINGLE_EVENT_ACTIVITY + "(a)";
        SWRLRule rule = SWRLRuleFactory.getSWRLRuleFromString(bodyString, headString, manager, ontology, pm);
        ontology.add(rule);

        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        final PelletExplanation explainer = new PelletExplanation(reasoner);

        Set<OWLAxiom> satisfiedAtoms = explainer.getInstanceExplanation(ind,
                singleEventClass);

        ontology.remove(rule);
        ontology.remove(subclassAxiom);
        return (satisfiedAtoms.size() > 0);
    }
}

package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.List;
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
import uk.ac.manchester.cs.owl.owlapi.SWRLVariableImpl;

public class ActionEvent {
    String type = "";
    String varName = "";
    List<String> dependentVars = new ArrayList<>();
    SWRLAtom predicateAtom = null;
    SWRLAtom startTimeAtom = null;
    SWRLAtom endTimeAtom = null;

    public ActionEvent() {

    }

    public ActionEvent(String type) {
        this.type = type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String getType() {
        return type;
    }

    public void setDependencies(List<SWRLAtom> atoms, String eventVarName) {
        atoms.forEach(atom -> {
            if (atom.allArguments().toArray().length < 2)
                return;

            List<String> vars = new ArrayList<>();
            atom.allArguments().forEach(arg -> {
                if (arg instanceof SWRLVariableImpl) {
                    IRI iri = (IRI) arg.components().findFirst().get();
                    vars.add(iri.getRemainder().get());
                }
            });

            if (vars.contains(eventVarName)) {
                vars.remove(eventVarName);
                this.dependentVars.addAll(vars);
            }
        });
    }

    public void setPredicateAtom(SWRLAtom atom) {
        this.predicateAtom = atom;
    }

    public SWRLAtom getPredicateAtom() {
        return predicateAtom;
    }

    public SWRLAtom getStartTimeAtom() {
        return startTimeAtom;
    }

    public void setStartTimeAtom(SWRLAtom startTimeAtom) {
        this.startTimeAtom = startTimeAtom;
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

    public SWRLAtom getEndTimeAtom() {
        return endTimeAtom;
    }

    public void setEndTimeAtom(SWRLAtom endTimeAtom) {
        this.endTimeAtom = endTimeAtom;
    }

    public List<String> getDependencies() {
        return dependentVars;
    }

    public boolean isEmpty() {
        return type.equals("") || (varName.equals("")) || (dependentVars.size() == 0);
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
        String bodyString = "Activity(a)^hasEvent(a,e)^" + this.type + "(e)";
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

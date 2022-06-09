package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLRule;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplInteger;
import uk.ac.manchester.cs.owl.owlapi.SWRLBuiltInAtomImpl;

public class EventConstraint {
    List<OWLFacetRestriction> restrections = new ArrayList<>();
    List<String> variables = new ArrayList<>();
    String operatorName = "";
    SWRLBuiltInAtomImpl operatorAtom = null;
    SWRLBuiltInAtomImpl greaterThanAtom = null;
    SWRLBuiltInAtomImpl lessThanAtom = null;
    List<ActionEvent> events = null;

    public EventConstraint(List<String> variables, String operatorName) {
        this.variables = variables;
        this.operatorName = operatorName;
    }

    public void setOperatorAtom(SWRLBuiltInAtomImpl atom) {
        operatorAtom = atom;
    }

    public void setGreaterThanAtom(SWRLBuiltInAtomImpl atom) {
        greaterThanAtom = atom;
    }

    public void setLessThanAtom(SWRLBuiltInAtomImpl atom) {
        lessThanAtom = atom;
    }

    public SWRLBuiltInAtomImpl getLessThanAtom() {
        return lessThanAtom;
    }

    public SWRLBuiltInAtomImpl getGreaterThanAtom() {
        return greaterThanAtom;
    }

    public void setEvents(List<ActionEvent> events) {
        this.events = events;
    }

    public List<ActionEvent> getEvents() {
        return events;
    }

    public String getMaxValue() {
        if (greaterThanAtom == null)
            return "value_not_found";

        SWRLDArgument arg = (SWRLDArgument) greaterThanAtom.getArguments().get(1);

        return arg.toString();
    }

    public String getMinValue() {
        if (greaterThanAtom == null)
            return "value_not_found";

        SWRLDArgument arg = (SWRLDArgument) greaterThanAtom.getArguments().get(1);

        return arg.toString();
    }

    public boolean isSatisfied(OWLNamedIndividual ind, OWLOntologyManager manager, OWLOntology ontology,
            PrefixManager pm) {

        // add the single event activity into the ontology
        OWLClass singleEventClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.SINGLE_EVENT_ACTIVITY,
                pm);
        OWLClass rootActivityClass = manager.getOWLDataFactory().getOWLClass(":" + ActivityList.ROOT_ACTIVITY, pm);
        OWLAxiom subclassAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(singleEventClass, rootActivityClass);
        ontology.add(subclassAxiom);

        String headString = ActivityList.SINGLE_EVENT_ACTIVITY + "(a)";
        String bodyString = "Activity(a)";
        for (ActionEvent event : events) {
            // event predicate
            bodyString += String.format("^%s(a,%s)^%s(%s)", Predicate.HAS_EVENT, event.varName, event.type,
                    event.varName);
            // start and end times
            if (event.getStartTimeAtom() != null) {
                bodyString += String.format("^%s(%s,%s)", Predicate.HAS_START_TIME, event.varName,
                        event.getStartTimeVar());
            }
            if (event.getEndTimeAtom() != null) {
                bodyString += String.format("^%s(%s,%s)", Predicate.HAS_END_TIME, event.varName,
                        event.getEndTimeVar());
            }
        }

        // built-in operator
        bodyString += "^" + operatorName + "(";
        for (String var : variables) {
            bodyString += var + ",";
        }
        bodyString = bodyString.substring(0, bodyString.length() - 1) + ")";

        // restrections
        if (greaterThanAtom != null) {
            OWLLiteralImplInteger limitValue = (OWLLiteralImplInteger) greaterThanAtom.getArguments().get(1)
                    .components()
                    .findFirst()
                    .get();
            bodyString += String.format("^greaterThan(%s,%s)", variables.get(0), limitValue.parseInteger());
        }

        if (lessThanAtom != null) {
            OWLLiteralImplInteger limitValue2 = (OWLLiteralImplInteger) lessThanAtom.getArguments().get(1)
                    .components()
                    .findFirst()
                    .get();
            bodyString += String.format("^lessThan(%s,%s)", variables.get(0), limitValue2.parseInteger());
        }

        SWRLRule rule = SWRLRuleFactory.getSWRLRuleFromString(bodyString, headString, manager, ontology, pm);
        ontology.add(rule);

        // check the rule entailment //TODO: internal reasoner might not be a good idea
        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        final PelletExplanation explainer = new PelletExplanation(reasoner);
        Set<OWLAxiom> satisfiedAtoms = explainer.getInstanceExplanation(ind, singleEventClass);

        ontology.remove(subclassAxiom);
        ontology.remove(rule);

        return (satisfiedAtoms.size() > 0);
    }

}

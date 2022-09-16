package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLRule;

public class OntologyDataManager {

    public static Map<String, List<String>> getOREvents(OWLOntology ontology, SWRLRule rule, PrefixManager pm,
            OWLDataFactory df) {

        List<String> targetEventGroups = getEventGroupsList(rule);

        Map<String, List<String>> ORevents = new HashMap<>();

        OWLClass eventClass = df.getOWLClass(":Event", pm);
        List<OWLSubClassOfAxiom> subEventClassAxioms = ontology.subClassAxiomsForSuperClass(eventClass)
                .collect(Collectors.toList());

        List<String> eventGroupMembers = new ArrayList<>();
        for (OWLSubClassOfAxiom ax : subEventClassAxioms) {
            String subEventName = ax.getSubClass().asOWLClass().getIRI().getShortForm();
            if (targetEventGroups.contains(subEventName)) {
                OWLClass eventGroupClass = ax.getSubClass().asOWLClass();
                OWLEquivalentClassesAxiom eventGroupSubClassAxioms = ontology.equivalentClassesAxioms(eventGroupClass)
                        .findFirst().get();

                for (OWLClass memberClass : new ArrayList<>(eventGroupSubClassAxioms.getClassesInSignature())) {
                    String evName = memberClass.getIRI().getShortForm();
                    if (!evName.equals(subEventName)) {
                        eventGroupMembers.add(evName);
                    }
                }

                if (eventGroupMembers.size() > 0) {
                    ORevents.put(subEventName, eventGroupMembers);
                }
            }
        }

        return ORevents;

    }

    public static Map<String, List<String>> getAllOREvents(OWLOntology ontology, PrefixManager pm,
            OWLDataFactory df) {

        Map<String, List<String>> ORevents = new HashMap<>();
        Set<SWRLRule> rules = ontology.getAxioms(AxiomType.SWRL_RULE);
        for (SWRLRule rule : rules) {
            ORevents.putAll(getOREvents(ontology, rule, pm, df));
        }

        return ORevents;
    }

    public static SWRLRule getRule(OWLOntology ontology, String activity) {

        List<OWLAxiom> axioms = new ArrayList<>(ontology.getAxioms());
        for (OWLAxiom axiom : axioms) {
            if (axiom instanceof SWRLRule) {
                SWRLRule rule = (SWRLRule) axiom;
                SWRLAtom act = rule.head().findFirst().get();
                OWLClass ruleHeadClass = (OWLClass) act.getPredicate();
                if (activity.equals(ruleHeadClass.getIRI().getShortForm())) {
                    return rule;
                }
            }
        }

        return null;
    }

    public static List<String> getEventGroupsList(SWRLRule rule) {
        List<String> eventGroups = new ArrayList<>();

        if (rule == null) {
            return eventGroups;
        }

        for (SWRLAtom atom : rule.bodyList()) {
            if (atom instanceof SWRLClassAtom) {
                OWLClass atomClass = (OWLClass) atom.getPredicate();
                String className = atomClass.getIRI().getShortForm();
                if (className.startsWith(Predicate.EVENTGROUP)) {
                    eventGroups.add(className);
                }
            }
        }
        return eventGroups;
    }

    public static OWLOntology eventGroupCleanup(List<String> eventGroups, List<List<String>> OREvents,
            OWLOntology ontology, OWLDataFactory df,
            PrefixManager pm) {
        for (String eventGroup : eventGroups) {
            OWLClass eventGroupClass = df.getOWLClass(":" + eventGroup, pm);

            OWLDeclarationAxiom decAx = df.getOWLDeclarationAxiom(eventGroupClass);
            ontology.remove(decAx);

            OWLClass eventClass = df.getOWLClass(":EVENT", pm);
            OWLSubClassOfAxiom subAx = df.getOWLSubClassOfAxiom(eventGroupClass, eventClass);
            ontology.remove(subAx);

            Set<OWLEquivalentClassesAxiom> eqAxs = ontology
                    .getEquivalentClassesAxioms(df.getOWLClass(":" + eventGroup, pm));

            eqAxs.forEach(ax -> {
                ontology.remove(ax);
            });

        }

        return ontology;
    }

}

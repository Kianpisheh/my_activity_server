package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;

public class OntologyDataManager {

    public static List<List<String>> getOREvents(OWLOntology ontology, PrefixManager pm, OWLDataFactory df) {

        List<List<String>> ORevents = new ArrayList<>();

        OWLClass eventClass = df.getOWLClass(":Event", pm);
        List<OWLSubClassOfAxiom> subEventClassAxioms = ontology.subClassAxiomsForSuperClass(eventClass)
                .collect(Collectors.toList());

        List<String> eventGroupMembers = new ArrayList<>();
        for (OWLSubClassOfAxiom ax : subEventClassAxioms) {
            String subEventName = ax.getSuperClass().asOWLClass().getIRI().getShortForm();
            if (subEventName.startsWith(Predicate.EVENTGROUP)) {
                List<OWLClass> eventGroupMembersClass = ax.getSubClass().classesInSignature()
                        .collect(Collectors.toList());
                eventGroupMembersClass.forEach(member -> eventGroupMembers.add(member.getIRI().getShortForm()));
                ORevents.add(eventGroupMembers);
            }
        }

        return ORevents;

    }

}

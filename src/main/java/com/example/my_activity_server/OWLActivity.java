package com.example.my_activity_server;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapitools.builders.BuilderSWRLRule;

public class OWLActivity {
    SWRLRule rule = null;
    String name = "";

    public OWLActivity() {

    }

    public OWLActivity(SWRLRule rule) {
        this.rule = rule;
        OWLClass activity = (OWLClass) rule.headList().get(0).getPredicate();
        name = activity.getIRI().getShortForm();
    }

    public SWRLRule getRule() {
        return rule;
    }

    public String getName() {
        return name;
    }

    public void addAtom(SWRLAtom atom, OWLDataFactory df) {
        BuilderSWRLRule builder = new BuilderSWRLRule(df);
        Stream<SWRLAtom> atoms = rule.body();
        Stream<SWRLAtom> newBody = Stream.concat(atoms, Stream.of(atom));
        builder.withBody(newBody);
        builder.withHead(rule.head());
        rule = builder.buildObject();
    }
}

package com.example.my_activity_server.API;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.example.my_activity_server.OWLActivity;
import com.example.my_activity_server.Predicate;
import com.example.my_activity_server.model.Activity;
import com.example.my_activity_server.model.ActivityInstance;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLBuiltInAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLDataPropertyAtom;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;
import org.semanticweb.owlapi.model.SWRLObjectPropertyAtom;
import org.semanticweb.owlapi.model.SWRLVariable;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLBuiltInAtomImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLClassAtomImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLObjectPropertyAtomImpl;
import uk.ac.manchester.cs.owl.owlapi.SWRLVariableImpl;

public class OwlToPojo {

    public static Activity getPojoActivity(OWLActivity owlActivity, int id) {
        List<SWRLAtom> atoms = owlActivity.getRule().bodyList();
        List<String> events = getEvents(atoms);

        List<ActionEventConstraintPojo> constraints = getTimeConstraints(atoms);

        // List<ActionEventConstraintPojo> constraints = getConstraints(atoms);

        return new Activity(id, owlActivity.getName(), events, constraints);
    }

    public static List<String> getEvents(List<SWRLAtom> atoms) {

        List<String> events = new ArrayList<>();

        atoms.forEach(atom -> {
            if (atom instanceof SWRLObjectPropertyAtomImpl) {
                OWLObjectPropertyImpl predicateClass = (OWLObjectPropertyImpl) atom.getPredicate();
                String PredicateName = predicateClass.getIRI().getShortForm();
                if (PredicateName.equals(Predicate.HAS_EVENT)) {
                    SWRLArgument eventArgument = (SWRLArgument) atom.allArguments().toArray()[1];
                    atoms.forEach(atom2 -> {
                        SWRLArgument atom2Arg = atom2.allArguments().findFirst().get();
                        // the ones that the first variable is "e"
                        if (atom2Arg.equals(eventArgument)) {
                            if (atom2 instanceof SWRLClassAtomImpl) { // event predicate
                                OWLClassImpl eventPredicate = (OWLClassImpl) atom2.getPredicate();
                                String predicateName = eventPredicate.getIRI().getShortForm();
                                events.add(predicateName);
                            }
                        }
                    });
                }
            }
        });

        return events;
    }

    public static List<ActionEventConstraintPojo> getTimeConstraints(List<SWRLAtom> atoms) {
        List<ActionEventConstraintPojo> constraintsPojo = new ArrayList<>();

        // get constraint vars
        List<String> constraintVars = getConstraintVar(atoms);
        for (String constraintVar : constraintVars) {
            Map<String, Integer> ths = getThresholds(constraintVar, atoms);
            List<String> events = getConstraintEvents(constraintVar, atoms);
            String constraintType = getConstraintType(constraintVar, atoms);
            if (constraintType.equals("duration")) {
                events.remove(1);
            }
            constraintsPojo.add(
                    new ActionEventConstraintPojo(constraintType, events, ths.get("greaterThan"), ths.get("lessThan")));
        }

        return constraintsPojo;
    }

    public static List<ActionEventConstraintPojo> getConstraints(List<SWRLAtom> atoms) {
        List<ActionEventConstraintPojo> constraintsPojo = new ArrayList<>();
        SWRLBuiltInAtomImpl greaterThanAtom = null;

        String operator = "";
        List<String> constraintEvents = new ArrayList<>();
        Map<Integer, String> constraintEvents2 = new HashMap<>();
        SortedMap<Integer, String> constraintEventsMap = new TreeMap<>();
        // find the constraint events
        List<Map<String, Object>> constraints = new ArrayList<>();
        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLBuiltInAtom) {
                IRI atomIRI = (IRI) atom.getPredicate();
                operator = atomIRI.getShortForm();
            } else {
                continue;
            }
            Map<String, Object> constraint = new HashMap<>();
            if (operator.equals("greaterThan") || operator.equals("lessThan")) {
                greaterThanAtom = (SWRLBuiltInAtomImpl) atom;
                SWRLLiteralArgument arg = (SWRLLiteralArgument) greaterThanAtom.getArguments().get(1);
                int th = arg.getLiteral().parseInteger();
                String thKey = "th2";
                if (operator.equals("greaterThan")) {
                    thKey = "th1";
                }
                constraint.put(thKey, th);
                // find the constraint vars
                SWRLVariable arg0 = (SWRLVariable) greaterThanAtom.getArguments().get(0);
                String constraintVar = arg0.getIRI().getShortForm(); // (e.g., "duration")
                constraint.put("var", constraintVar);
                List<String> constraintDepVars = findDependentVariables(constraintVar, atoms);
                for (String constraintDepVar : constraintDepVars) { // (e.g., var: t1, t2)
                    // (e.g., vars: e_t, e_sh, and duration)
                    List<String> eventVars = findDependentVariables(constraintDepVar, atoms);
                    eventVars.remove(constraintVar); // remove the constraint var itself
                    eventVars.removeIf(ev -> constraintDepVars.contains(ev)); // remove (t1, t2, ...)
                    if (eventVars.size() > 1) {
                        System.err.println(
                                "More than one event is mapped to a temporal variable (e.g., start time, and end time)");
                    }
                    String event = findEventFromVar(eventVars.get(0), atoms);
                    int p = findPlaceInOperation(eventVars.get(0), constraintDepVar, atoms, "subtract");
                    if (event != null && (p >= 0) && (p <= 2)) {
                        constraintEvents.add(event);
                        constraintEvents2.put(p, event);
                        constraintEventsMap.put(p, event);
                    }
                }

                constraintEvents = new ArrayList<String>(constraintEventsMap.values());

                Set<String> constraintEventsSet = new HashSet<String>(constraintEvents);
                String type = null;
                if (constraintEventsSet.size() == 1) {
                    type = "duration";
                } else if (constraintEventsSet.size() == 2) {
                    type = "time_distance";
                }
                constraint.put("type", type);
                constraint.put("events", constraintEventsSet);
            }
            if (constraint.size() != 0)
                constraints.add(constraint);
        }
        // merge sibling constraints
        constraints = mergeSiblingConstraints(constraints);

        for (Map<String, Object> constraint : constraints) {
            Set<String> s = (Set<String>) constraint.get("events");
            if (s.size() > 0) {
                constraintsPojo.add(new ActionEventConstraintPojo((String) constraint.get("type"), new ArrayList<>(s),
                        (int) constraint.get("th1"), (int) constraint.get("th2")));
            }
        }

        return constraintsPojo;
    }

    private static List<String> findDependentVariables(String var, List<SWRLAtom> atoms) {
        List<String> vars = new ArrayList<>();
        for (SWRLAtom atom : atoms) {
            atom.allArguments().forEach(arg -> {
                if (arg instanceof SWRLVariableImpl) {
                    IRI iri = (IRI) arg.components().findFirst().get();
                    if (var.equals(iri.getRemainder().get())) {
                        atom.allArguments().forEach(arg2 -> {
                            if (arg2 instanceof SWRLVariableImpl) {
                                IRI iri2 = (IRI) arg2.components().findFirst().get();
                                if (!var.equals(iri2.getRemainder().get())) {
                                    vars.add(iri2.getRemainder().get());
                                }
                            }
                        });
                    }
                }
            });
        }

        return vars;
    }

    private static String findEventFromVar(String var, List<SWRLAtom> atoms) {

        String event = null;

        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLClassAtomImpl) {
                SWRLVariable eventArg = (SWRLVariable) atom.allArguments().findFirst().get();
                if (var.equals(eventArg.getIRI().getRemainder().get())) {
                    OWLClassImpl eventClass = (OWLClassImpl) atom.getPredicate();
                    event = eventClass.getIRI().getShortForm();
                }
            }
        }

        return event;
    }

    private static List<Map<String, Object>> mergeSiblingConstraints(List<Map<String, Object>> constraints) {
        List<Map<String, Object>> mergedConstraints = new ArrayList<>();

        List<Integer> touched = new ArrayList<>();
        for (int i = 0; i < constraints.size() - 1; i++) {
            if (touched.contains(i)) {
                continue;
            }
            for (int j = i + 1; j < constraints.size(); j++) {
                @SuppressWarnings("unchecked")
                Set<String> intersectionEvents = new HashSet<>(
                        (Set<String>) constraints.get(i).get("events"));

                @SuppressWarnings("unchecked")
                Set<String> eventSet2 = (Set<String>) constraints.get(j).get("events");
                intersectionEvents.retainAll(eventSet2);
                if (intersectionEvents.size() == eventSet2.size()) {
                    if (constraints.get(i).get("var").equals(constraints.get(j).get("var"))) {
                        if (constraints.get(i).containsKey("th1")) {
                            constraints.get(i).put("th2", constraints.get(j).get("th2"));
                        } else {
                            constraints.get(i).put("th1", constraints.get(j).get("th1"));
                        }
                        mergedConstraints.add(constraints.get(i));
                        touched.add(j);
                    }
                }
            }
        }

        return mergedConstraints;

    }

    private static int findPlaceInOperation(String eventVar, String constraintDepVar, List<SWRLAtom> atoms,
            String operator) {

        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLBuiltInAtom) {
                IRI iri = (IRI) atom.getPredicate();
                if (iri.getShortForm().equals(operator)) {
                    List<SWRLArgument> vars = (List<SWRLArgument>) atom.getAllArguments();
                    SWRLVariable var0 = (SWRLVariable) vars.get(1);
                    SWRLVariable var1 = (SWRLVariable) vars.get(2);
                    if (var0.getIRI().getShortForm().equals(constraintDepVar)) {
                        return 1;
                    } else if (var1.getIRI().getShortForm().equals(constraintDepVar)) {
                        return 2;
                    }
                }
            }
        }
        return -1;
    }

    public static ActivityInstance getActivityInstances() {

        return null;
    }

    private static List<String> getConstraintVar(List<SWRLAtom> atoms) {
        List<String> constraintVars = new ArrayList<>();
        String operator = "";
        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLBuiltInAtom) {
                IRI atomIRI = (IRI) atom.getPredicate();
                operator = atomIRI.getShortForm();
                if (operator.equals("subtract")) {
                    // get constraint var
                    SWRLBuiltInAtomImpl subtractAtom = (SWRLBuiltInAtomImpl) atom;
                    SWRLVariable arg0 = (SWRLVariable) subtractAtom.getArguments().get(0);
                    String constraintVar = arg0.getIRI().getShortForm(); // (e.g., "duration")
                    constraintVars.add(constraintVar);
                }
            }
        }

        return constraintVars;
    }

    private static Map<String, Integer> getThresholds(String constraintVar, List<SWRLAtom> atoms) {
        Map<String, Integer> ths = new HashMap<>();
        String operator = "";

        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLBuiltInAtom) {
                IRI atomIRI = (IRI) atom.getPredicate();
                operator = atomIRI.getShortForm();
                if (operator.equals("greaterThan") || operator.equals("lessThan")) {
                    SWRLBuiltInAtomImpl limitAtom = (SWRLBuiltInAtomImpl) atom;
                    SWRLVariable arg1 = (SWRLVariable) limitAtom.getArguments().get(0);
                    if (arg1.getIRI().getShortForm().equals(constraintVar)) {
                        SWRLLiteralArgument arg2 = (SWRLLiteralArgument) limitAtom.getArguments().get(1);
                        int th = arg2.getLiteral().parseInteger();
                        ths.put(operator, th);
                    }
                }
            }
        }

        return ths;
    }

    private static String getConstraintType(String constraintVar, List<SWRLAtom> atoms) {
        Map<String, String> timeVars = getEventTimeVars(constraintVar, atoms);
        Map<String, String> timeToEventVars = getEventVarsFromTimeVars(timeVars, atoms);

        List<String> eventVars = new ArrayList(timeToEventVars.values());

        if (eventVars.get(0).equals(eventVars.get(1))) {
            return "duration";
        } else {
            return "time_distance";

        }
    }

    // [Pot, Fridge], t_Pot - t_Fridge
    private static List<String> getConstraintEvents(String constraintVar, List<SWRLAtom> atoms) {
        List<String> events = new ArrayList<>();
        Map<String, String> timeVars = getEventTimeVars(constraintVar, atoms);
        Map<String, String> timeToEventVars = getEventVarsFromTimeVars(timeVars, atoms);
        Map<String, String> timeToEvent = getEvents(timeToEventVars, atoms);

        events.add(timeToEvent.get("t1"));
        events.add(timeToEvent.get("t2"));
        return events;
    }

    // {t1: e1, t2: e2} --> {t1: Mug, t2: Kettle}
    private static Map<String, String> getEvents(Map<String, String> timeToEventVars, List<SWRLAtom> atoms) {

        Map<String, String> timeToEvent = new HashMap<>();

        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLClassAtom) {
                SWRLClassAtom classAtom = (SWRLClassAtom) atom;
                SWRLVariable classArg = (SWRLVariable) classAtom.getArgument();
                String classVar = classArg.getIRI().getShortForm();
                if (timeToEventVars.containsValue(classVar)) {
                    for (String timeVar : timeToEventVars.keySet()) {
                        if (timeToEventVars.get(timeVar).equals(classVar)) {
                            String event = classAtom.getPredicate().asOWLClass().getIRI().getShortForm();
                            timeToEvent.put(timeVar, event);
                        }
                    }
                }
            }
        }

        return timeToEvent;
    }

    // {t1: e1, t2: e2}
    private static Map<String, String> getEventVarsFromTimeVars(Map<String, String> times, List<SWRLAtom> atoms) {
        Map<String, String> timeToEvent = new HashMap<>();

        String dataProp = "";
        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLDataPropertyAtom) {
                SWRLDataPropertyAtom dataPropAtom = (SWRLDataPropertyAtom) atom;
                dataProp = dataPropAtom.getPredicate().asOWLDataProperty().getIRI().getShortForm();
                if (dataProp.equals(Predicate.HAS_END_TIME) || dataProp.equals(Predicate.HAS_START_TIME)) {
                    SWRLVariable arg2 = (SWRLVariable) dataPropAtom.getSecondArgument();
                    String timeVar = arg2.getIRI().getShortForm();
                    if (times.containsValue(timeVar)) {
                        for (String timeKey : times.keySet()) {
                            if (times.get(timeKey).equals(timeVar)) {
                                SWRLVariable arg1 = (SWRLVariable) dataPropAtom.getFirstArgument();
                                String eventVar = arg1.getIRI().getShortForm();
                                timeToEvent.put(timeKey, eventVar);
                            }
                        }
                    }

                }
            }
        }

        return timeToEvent;
    }

    private static Map<String, String> getEventTimeVars(String constraintVar, List<SWRLAtom> atoms) {

        Map<String, String> times = new HashMap<>();

        String operator = "";
        for (SWRLAtom atom : atoms) {
            if (atom instanceof SWRLBuiltInAtom) {
                IRI atomIRI = (IRI) atom.getPredicate();
                operator = atomIRI.getShortForm();
                if (operator.equals("subtract")) {
                    SWRLBuiltInAtomImpl subtractAtom = (SWRLBuiltInAtomImpl) atom;
                    SWRLVariable arg0 = (SWRLVariable) subtractAtom.getArguments().get(0);
                    if (arg0.getIRI().getShortForm().equals(constraintVar)) {
                        SWRLVariable arg1 = (SWRLVariable) subtractAtom.getArguments().get(1);
                        SWRLVariable arg2 = (SWRLVariable) subtractAtom.getArguments().get(2);
                        times.put("t1", arg2.getIRI().getShortForm());
                        times.put("t2", arg1.getIRI().getShortForm());
                        return times;
                    }
                }
            }
        }

        return times;
    }

}

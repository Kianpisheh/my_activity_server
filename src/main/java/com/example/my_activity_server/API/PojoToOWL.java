package com.example.my_activity_server.API;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLRule;

import com.example.my_activity_server.OntologyDataManager;
import com.example.my_activity_server.Predicate;

public class PojoToOWL {

    public static String createSwrlRuleBodyString(Map<String, Object> activity, OWLOntology ontology, PrefixManager pm,
            OWLDataFactory df) {

        String bodyString = "Activity(a)";

        // check if the events format is correct
        Object events = activity.get("events");
        if (events instanceof List<?>) {
            for (Object ev : (List<?>) events) {
                if (!(ev instanceof String)) {
                    System.err.println("The activity events is not in the proper format (i.e., List<String>)");
                    return null;
                }
            }
        }

        List<Map<String, Object>> constraints = (List<Map<String, Object>>) activity.get("constraints");

        // add the extra event of "same interaction time distance" axiom (hasEvent for
        // that extra event)
        List<String> events2 = (List<String>) events;
        for (int i = 0; i < constraints.size(); i++) {
            List<String> consEvents = (List<String>) constraints.get(i).get("events");
            if (constraints.get(i).get("type").equals("time_distance")) {
                if (consEvents.get(0).equals(consEvents.get(1))) {
                    events2.add(consEvents.get(0));
                }
            }
        }

        // add events predicates
        for (int i = 0; i < events2.size(); i++) {
            bodyString += String.format("^%s(a,e_%s)^%s(e_%s)", Predicate.HAS_EVENT, i, events2.get(i), i);
        }

        // add events exclusion predicates
        List<String> excludedEvents = (List<String>) activity.get("excludedEvents");
        for (int i = events2.size(); i < excludedEvents.size() + events2.size(); i++) {
            bodyString += String.format("^%s(a,e_%s)^%s(e_%s)", Predicate.HAS_NOT_EVENT, i,
                    excludedEvents.get(i - events2.size()),
                    i);
        }

        // add ORevents predicates
        Map<String, List<String>> allOREvents = OntologyDataManager.getAllOREvents(ontology, pm, df);
        List<List<String>> OREvents = (List<List<String>>) activity.get("eventORList");
        String activityName = (String) activity.get("name");
        SWRLRule rule = OntologyDataManager.getRule(ontology, activityName);
        Map<String, List<String>> currentOREvents = OntologyDataManager.getOREvents(ontology, rule, pm, df);
        for (int i = 0; i < OREvents.size(); i++) {
            List<String> evs = OREvents.get(i);
            // it has to be an axiom of "this" activity
            String eventGroupName = getEventGroupName(currentOREvents, evs);
            if (eventGroupName.equals("")) {
                // create a new eventGroup name
                eventGroupName = getUniqueEventGroupName(allOREvents);
            }
            allOREvents.put(eventGroupName, evs);
            bodyString += String.format("^%s(a,eg_%s)^%s(eg_%s)", Predicate.HAS_EVENT, i, eventGroupName, i);
        }

        // add constraints predicates
        for (int i = 0; i < constraints.size(); i++) {
            List<String> constraintEvents = (List<String>) constraints.get(i).get("events");
            List<Integer> opSize = (List<Integer>) constraints.get(i).get("opSize");

            String event1 = constraintEvents.get(0);
            String event2 = event1;

            String constraintType = (String) constraints.get(i).get("type");
            if (constraintType.contains("or")) {
                List<String> eventsG = findEventGroup(currentOREvents, constraintEvents, opSize);
                event1 = eventsG.get(0);
                if (eventsG.size() > 1) {
                    event2 = eventsG.get(1);
                }
            }

            if (constraints.get(i).get("type").equals("time_distance")) {
                event2 = constraintEvents.get(1);
            }

            int idx1 = 0;
            int idx2 = 0;
            if (event1.equals(event2)) { // same type interactions
                idx2 = 1;
            }

            String evVar1 = retrieveEventVar(event1, bodyString, idx1);
            String evVar2 = evVar1;
            String consType = (String) constraints.get(i).get("type");
            if (consType.contains("time_distance")) {
                evVar2 = retrieveEventVar(event2, bodyString, idx2); // same type interactions
            }

            bodyString += String.format("^hasStartTime(%s,t1_%s)^hasEndTime(%s,t2_%s)", evVar2, evVar2, evVar1, evVar1);

            // add builtitn operator
            String t2 = "t2_" + evVar1;
            String t1 = "t1_" + evVar1;
            String type = (String) constraints.get(i).get("type");
            if (type.contains("time_distance")) {
                t2 = "t1_" + evVar2;
                t1 = "t2_" + evVar1;
            }
            bodyString += String.format("^subtract(%s_%s,%s,%s)", type, i, t2, t1);

            // add builtin data range atoms
            if (constraints.get(i).get("th1") != null) {
                bodyString += String.format("^greaterThan(%s_%s,%s)", type, i, constraints.get(i).get("th1"));
            }
            if (constraints.get(i).get("th2") != null) {
                bodyString += String.format("^lessThan(%s_%s,%s)", type, i, constraints.get(i).get("th2"));
            }
        }

        return bodyString;
    }

    private static String retrieveEventVar(String event, String bodyString, int idx) {
        String[] ss = bodyString.split(event + "[(]"); // retireve the event var name
        int eg = 0;
        if (event.contains("EventGroup")) {
            eg = 1;
        }
        String eventVar = ss[1 + idx].substring(0, 3 + eg); // (e.g., e_0)
        return eventVar;
    }

    private static List<String> findEventGroup(Map<String, List<String>> currentOREvents, List<String> constraintEvents,
            List<Integer> opSize) {

        List<String> eventsG = new ArrayList<>();

        List<String> subEvents = constraintEvents.subList(0, opSize.get(0));
        if (subEvents.size() == 1) {
            eventsG.add(subEvents.get(0));
        } else {
            for (String evGroup : currentOREvents.keySet()) {
                List<String> l = currentOREvents.get(evGroup);
                Collections.sort(l, Collator.getInstance());
                Collections.sort(subEvents, Collator.getInstance());
                if (l.equals(subEvents)) {
                    eventsG.add(evGroup);
                }
            }
        }

        if (opSize.size() > 1) {
            List<String> subEvents2 = constraintEvents.subList(opSize.get(0), opSize.get(0) + opSize.get(1));
            if (subEvents2.size() == 1) {
                eventsG.add(subEvents2.get(0));
            } else {
                for (String evGroup : currentOREvents.keySet()) {
                    List<String> l = currentOREvents.get(evGroup);
                    Collections.sort(l, Collator.getInstance());
                    Collections.sort(subEvents2, Collator.getInstance());
                    if (l.equals(subEvents2)) {
                        eventsG.add(evGroup);
                    }
                }
            }
        }

        return eventsG;
    }

    private static String getEventGroupName(Map<String, List<String>> ontologyOREvents, List<String> OREvents) {
        for (String evGroupName : ontologyOREvents.keySet()) {
            if (ontologyOREvents.get(evGroupName).containsAll(OREvents)
                    || OREvents.containsAll(ontologyOREvents.get(evGroupName))) {
                return evGroupName;
            }
        }

        return "";
    }

    private static String getUniqueEventGroupName(Map<String, List<String>> ontologyOREvents) {

        int i = 0;
        int[] ids = new int[ontologyOREvents.size() + 1];
        ids[0] = 1;
        for (String name : ontologyOREvents.keySet()) {
            String idStr = name.split(Predicate.EVENTGROUP, 2)[1];
            ids[i] = Integer.valueOf(idStr);
            i += 1;
        }

        Arrays.sort(ids);
        return String.format(Predicate.EVENTGROUP + "%s", ids[ids.length - 1] + 1);
    }

}

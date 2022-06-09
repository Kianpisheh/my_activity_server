package com.example.my_activity_server.API;

import java.util.List;
import java.util.Map;

import com.example.my_activity_server.Predicate;

public class PojoToOWL {

    public static String createSwrlRuleBodyString(Map<String, Object> activity) {

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

        // add events predicates
        List<String> events2 = (List<String>) events;
        for (int i = 0; i < events2.size(); i++) {
            bodyString += String.format("^%s(a,e_%s)^%s(e_%s)", Predicate.HAS_EVENT, i, events2.get(i), i);
        }
        // add constraints predicates
        List<Map<String, Object>> constraints = (List<Map<String, Object>>) activity.get("constraints");
        for (int i = 0; i < constraints.size(); i++) {
            List<String> constraintEvents = (List<String>) constraints.get(i).get("events");
            String event1 = constraintEvents.get(0);
            String event2 = event1;
            if (constraintEvents.size() == 2) {
                event2 = constraintEvents.get(1);
            }
            String evVar1 = retrieveEventVar(event1, bodyString);
            String evVar2 = retrieveEventVar(event2, bodyString);
            bodyString += String.format("^hasStartTime(%s,t1_%s)^hasEndTime(%s,t2_%s)", evVar2, evVar2, evVar1, evVar1);

            // add builtitn operator
            String t2 = "t2_" + evVar1;
            String t1 = "t1_" + evVar1;
            String type = (String) constraints.get(i).get("type");
            if (type.equals("time_distance")) {
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

    private static String retrieveEventVar(String event, String bodyString) {
        String[] ss = bodyString.split(event + "[(]"); // retireve the event var name
        String eventVar = ss[1].substring(0, 3); // (e.g., e_0)
        return eventVar;
    }
}

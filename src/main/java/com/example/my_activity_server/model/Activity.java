package com.example.my_activity_server.model;

import java.util.List;

import com.example.my_activity_server.API.ActionEventConstraintPojo;

public class Activity {
    String name;
    int id;
    List<String> events;
    List<String> excludedEvents;
    List<ActionEventConstraintPojo> constraints;

    public Activity() {
    }

    public Activity(int id, String name, List<String> events, List<String> excludedEvents,
            List<ActionEventConstraintPojo> constraints) {
        this.id = id;
        this.name = name;
        this.events = events;
        this.excludedEvents = excludedEvents;
        this.constraints = constraints;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getEvents() {
        return events;
    }

    public List<String> getExcludedEvents() {
        return excludedEvents;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public void setExcludedEventsEvents(List<String> excludedEvents) {
        this.excludedEvents = excludedEvents;
    }

    public List<ActionEventConstraintPojo> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ActionEventConstraintPojo> constraints) {
        this.constraints = constraints;
    }

    @Override
    public String toString() {
        return "Activity [id=" + id + ", name=" + name + ", events=" + events + "]";
    }

}

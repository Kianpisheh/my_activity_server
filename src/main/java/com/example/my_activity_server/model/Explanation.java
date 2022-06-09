package com.example.my_activity_server.model;

import java.util.List;

import com.example.my_activity_server.API.ActionEventConstraintPojo;

public class Explanation {
    List<String> events;
    List<ActionEventConstraintPojo> constraints;

    public Explanation() {
    }

    public Explanation(List<String> events, List<ActionEventConstraintPojo> constraints) {
        this.events = events;
        this.constraints = constraints;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public List<ActionEventConstraintPojo> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ActionEventConstraintPojo> constraints) {
        this.constraints = constraints;
    }

}

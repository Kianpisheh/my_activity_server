package com.example.my_activity_server.model;

import java.util.List;

public class ActivityInstance {
    String name;
    String type;
    List<EventInstance> events;

    public ActivityInstance(String name, String type, List<EventInstance> events) {
        this.name = name;
        this.type = type;
        this.events = events;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<EventInstance> getEvents() {
        return events;
    }

    public void setEvents(List<EventInstance> events) {
        this.events = events;
    }

}

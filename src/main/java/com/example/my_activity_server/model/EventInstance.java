package com.example.my_activity_server.model;

public class EventInstance {
    String name;
    String type;
    double startTime = -1;
    double endTime = -1;

    public EventInstance() {

    }

    public EventInstance(String name) {
        this.name = name;
    }

    public EventInstance(String name, String type, double startTime, double endTime) {
        this.name = name;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

}

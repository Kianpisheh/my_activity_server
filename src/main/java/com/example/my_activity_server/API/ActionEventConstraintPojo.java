package com.example.my_activity_server.API;

import java.util.ArrayList;
import java.util.List;

public class ActionEventConstraintPojo {
    String type = "";
    List<String> events = null;
    int th1 = -1;
    int th2 = -1;
    List<Integer> opSize = new ArrayList<>();

    public ActionEventConstraintPojo() {
    }

    public ActionEventConstraintPojo(String type, List<String> events, int th1, int th2) {
        this.type = type;
        this.events = events;
        this.th1 = th1;
        this.th2 = th2;
    }

    public ActionEventConstraintPojo(String type, List<String> events, int th1, int th2, List<Integer> opSize) {
        this.type = type;
        this.events = events;
        this.th1 = th1;
        this.th2 = th2;
        this.opSize = opSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public int getTh1() {
        return th1;
    }

    public void setTh1(int th1) {
        this.th1 = th1;
    }

    public int getTh2() {
        return th2;
    }

    public void setTh2(int th2) {
        this.th2 = th2;
    }

    public List<Integer> getOpSize() {
        return opSize;
    }

    public void setOpSize(List<Integer> opSize) {
        this.opSize = opSize;
    }

}

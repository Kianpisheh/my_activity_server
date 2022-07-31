package com.example.my_activity_server.model;

public class RuleItem {
    float conf;
    float supp;
    String[] items;

    public RuleItem(float conf, float supp, String[] items) {
        this.conf = conf;
        this.supp = supp;
        this.items = items;
    }

    public float getConf() {
        return conf;
    }

    public float getSupp() {
        return supp;
    }

    public String[] getItems() {
        return items;
    }

    public void setConf(float conf) {
        this.conf = conf;
    }

    public void setSupp(float supp) {
        this.supp = supp;
    }

    public void setItems(String[] items) {
        this.items = items;
    }

}

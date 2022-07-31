package com.example.my_activity_server.model;

import java.util.List;

public class ActivityRuleItem {
    List<RuleItem> ruleitems;
    String activity;

    public ActivityRuleItem(List<RuleItem> ruleitems, String activity) {
        this.ruleitems = ruleitems;
        this.activity = activity;
    }

}

package com.example.my_activity_server.controller;

import java.util.List;
import java.util.Map;

import com.example.my_activity_server.model.Activity;
import com.example.my_activity_server.service.ActivityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(path = "/activity")
class ActivityController {

    private final ActivityService activityService;

    @Autowired
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public List<Activity> getActivity(@RequestBody Map<String, String> dataset) {
        return activityService.getActivities(dataset.get("dataset") + "-" + dataset.get("user"));
    }

    @PostMapping(value = "/update")
    public void updateActivity(@RequestBody Map<String, Object> data) {
        Map<String, Object> activity = (Map<String, Object>) data.get("activity");
        activityService.updateActivity(activity, data.get("dataset") + "-" + data.get("user"));
    }

    @PostMapping(value = "/remove")
    public void addActivity(@RequestBody Map<String, Object> data) {
        Map<String, Object> activity = (Map<String, Object>) data.get("activity");
        String activityName = (String) activity.get("name");
        activityService.removeActivity(activityName.replace("=", ""), data.get("dataset") + "-" + data.get("user"));
    }
}
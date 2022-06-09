package com.example.my_activity_server.controller;

import java.util.List;
import java.util.Map;

import com.example.my_activity_server.model.Activity;
import com.example.my_activity_server.service.ActivityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/activity")
class ActivityController {

    private final ActivityService activityService;

    @Autowired
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public List<Activity> getActivity() {
        return activityService.getActivities();
    }

    @PostMapping(value = "/update")
    public void updateActivity(@RequestBody Map<String, Object> activity) {
        activityService.updateActivity(activity);
    }

    @PostMapping(value = "/remove")
    public void addActivity(@RequestBody String activity) {
        activityService.removeActivity(activity.replace("=", ""));
    }
}
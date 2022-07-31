package com.example.my_activity_server.controller;

import java.util.List;

import com.example.my_activity_server.model.ActivityInstance;
import com.example.my_activity_server.service.ActivityInstanceService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/instance")
public class ActivityInstanceController {

    private final ActivityInstanceService activityInstanceService;

    public ActivityInstanceController(ActivityInstanceService activityInstanceService) {
        this.activityInstanceService = activityInstanceService;
    }

    @GetMapping(value = "/instances")
    public List<ActivityInstance> getInstances() {
        return activityInstanceService.getActivityInstances();
    }

    @PostMapping(value = "/classify")
    public List<List<String>> classify(@RequestBody List<String> activityInstances) {
        return activityInstanceService.classify(activityInstances);
    }
}

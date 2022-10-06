package com.example.my_activity_server.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.my_activity_server.service.EventLogService;
import com.google.gson.JsonObject;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(path = "")
public class EventLogController {

    private final EventLogService eventLogService;

    @Autowired
    public EventLogController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @PostMapping(value = "/log_event")
    public void logEvent(@RequestBody Map<String, String> data) {
        String event = data.get("event");
        String dataset = data.get("dataset");
        eventLogService.logEvent(event, dataset);
    }

}

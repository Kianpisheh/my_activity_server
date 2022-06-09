package com.example.my_activity_server.controller;

import java.util.Map;

import com.example.my_activity_server.service.ExplainerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/explainer")
public class ExplainerController {

    private final ExplainerService explainerService;

    @Autowired
    public ExplainerController(ExplainerService explainerService) {
        this.explainerService = explainerService;
    }

    @PostMapping(value = "/explain")
    public Map<String, Object> why(@RequestBody Map<String, String> query) {
        System.out.println(query);
        if (query.get("type").equals("why?")) {
            return explainerService.why(query);
        } else if (query.get("type").equals("why not?")) {
            return explainerService.whyNot(query);
        }

        return null;
    }

}

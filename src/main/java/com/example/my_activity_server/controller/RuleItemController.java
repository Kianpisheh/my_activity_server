package com.example.my_activity_server.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.my_activity_server.model.RuleItem;
import com.example.my_activity_server.service.RuleItemService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/ruleitems")
public class RuleItemController {
    private final RuleItemService ruleItemService;

    public RuleItemController(RuleItemService ruleItemService) {
        this.ruleItemService = ruleItemService;
    }

    @GetMapping(value = "/get_ruleitems")
    public Map<String, List<RuleItem>> getRuleitems() {
        return ruleItemService.getRuleitems();
    }
}

package com.example.my_activity_server.service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.my_activity_server.model.RuleItem;
import com.google.gson.Gson;

@Service
public class RuleItemService {

    public Map<String, List<RuleItem>> getRuleitems() {

        // read the mined objects ruleitems in the dataset
        Gson gson = new Gson();
        Map<String, List<RuleItem>> actRuleitems = new HashMap<>();
        try {
            actRuleitems = (Map<String, List<RuleItem>>) gson.fromJson(new FileReader("rules.json"), Object.class);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        return actRuleitems;
    }

}

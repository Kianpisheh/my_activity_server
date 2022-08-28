package com.example.my_activity_server.service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.text.Document;

import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.my_activity_server.model.ActivityInstance;
import com.example.my_activity_server.model.RuleItem;
import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

@Service
public class RuleItemService {

    @Autowired
    private ActivityService activityService;

    public Map<String, List<RuleItem>> getRuleitems(Map<String, String> dataset) {
        Map<String, List<RuleItem>> actRuleitems = new HashMap<>();
        Gson gson = new Gson();

        // read the mined objects ruleitems in the dataset

        List<ActivityInstance> activityInstances = new ArrayList<>();
        MongoDatabase db = activityService.getDatabase();
        MongoCollection ruleitemsCol = db.getCollection("ruleitems");

        Bson query = eq("_id", dataset.get("dataset") + "-ruleitems");
        FindIterable<Document> iterDoc = ruleitemsCol.find(query);
        Iterator it = iterDoc.iterator();
        Map<String, Object> object = (Map<String, Object>) it.next();
        Set<String> activities = object.keySet();
        for (String act : activities) {
            if (act.equals("_id")) {
                continue;
            }
            List<Map<String, Object>> ruleitems = (List<Map<String, Object>>) object.get(act);
            List<RuleItem> rules = new ArrayList<>();
            for (Map<String, Object> ruleitemObj : ruleitems) {
                double confd = (double) ruleitemObj.get("conf");
                float conf = (float) confd;
                double suppd = (double) ruleitemObj.get("supp");
                float supp = (float) suppd;
                List<String> itemsL = (List<String>) ruleitemObj.get("items");
                String[] items = new String[itemsL.size()];
                for (int i = 0; i < itemsL.size(); i++) {
                    items[i] = itemsL.get(i);
                }
                rules.add(new RuleItem(conf, supp, items));
            }
            actRuleitems.put(act, new ArrayList<>(rules));
        }

        return actRuleitems;
    }

}

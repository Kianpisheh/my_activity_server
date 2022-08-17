package com.example.my_activity_server.service;

import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import static com.mongodb.client.model.Filters.eq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Service
public class LoginService {

    @Autowired
    private ActivityService activityService;

    public Boolean checkPassword(Map<String, String> userData) {
        String queriedUsername = (String) userData.get("user");
        String queriedPass = (String) userData.get("pass");

        MongoDatabase db = activityService.getDatabase();
        MongoCollection usersCollection = db.getCollection("users");
        Bson query = eq("username", queriedUsername);
        FindIterable<Document> iterDoc = usersCollection.find(query);
        Document user = iterDoc.first();
        String pass = (String) user.get("pass");

        return pass.equals(queriedPass);
    }

}

package com.example.my_activity_server.service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@Service
public class EventLogService {

    public void logEvent(String dataString, String dataset) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonElement je = JsonParser.parseString(dataString);
        String prettyJsonString = gson.toJson(je);

        Path filePath = Path.of("../data/log/" + dataset + "-log.txt");
        try (FileWriter fileWriter = new FileWriter(filePath.toFile(), true)) {
            fileWriter.write(prettyJsonString);
            fileWriter.write("\n##################################\n");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

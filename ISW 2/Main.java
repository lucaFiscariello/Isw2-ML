package com.fiscariello;

import java.io.FileReader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fiscariello.ml.WalkForward;


public class Main {
    public static void main(String[] args) throws Exception {
        
        String projectName="AVRO";
        String jiraTicket="AVRO-";
        double endperc=0.5;
        double startperc=0.05;

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader("Configuration.json"));
        JSONObject jsonObject =  (JSONObject) obj;
        String repoPath = (String) jsonObject.get(projectName);
        
        WalkForward wf = new WalkForward(repoPath, projectName, jiraTicket, endperc, startperc);
        wf.execute();

    }





}

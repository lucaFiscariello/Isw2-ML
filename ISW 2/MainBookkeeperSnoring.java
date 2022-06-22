package com.fiscariello;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fiscariello.dataset.DatasetCreator;
import com.fiscariello.project.ProjectInfo;
import com.fiscariello.project.Release;

import tech.tablesaw.api.Table;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.core.Instances;
import weka.core.converters.CSVLoader;


public class MainBookkeeperSnoring {
    public static void main(String[] args) throws Exception {

        String projectName="BOOKKEEPER";
        String jiraTicket="BOOKKEEPER-";
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader("Configuration.json"));
        JSONObject jsonObject =  (JSONObject) obj;
        String repoPath = (String) jsonObject.get(projectName);
        
        ProjectInfo projectInfo = new ProjectInfo(projectName, repoPath, jiraTicket);
        
        splitDataset(projectInfo);        
        evalModel(projectInfo);

    }

    private static List<Double> evalModel(ProjectInfo projectInfo) throws Exception {

        double endperc=0.5;
        int size= projectInfo.getReleasesByPerc(endperc).size();
        int numTrainRelease =1;
        ArrayList<Double> precision = new ArrayList<>();
        
        
        for(;numTrainRelease<size-2; numTrainRelease++){
            String datasetTrain = "train"+numTrainRelease+".csv";
            String datasetTest = "test"+numTrainRelease+".csv";

            CSVLoader loaderTrain = new CSVLoader();
            loaderTrain.setSource(new File(datasetTrain));
            Instances datatrain = loaderTrain.getDataSet();

            CSVLoader loaderTest = new CSVLoader();
            loaderTest.setSource(new File(datasetTest));
            Instances datatest = loaderTest.getDataSet();

            int numAttr = datatrain.numAttributes();
            		
		    datatrain.setClassIndex(numAttr - 1);
		    datatest.setClassIndex(numAttr - 1);

			Classifier classifier = new IBk();
		    classifier.buildClassifier(datatrain);

		    Evaluation eval = new Evaluation(datatest);	

            try{
                eval.evaluateModel(classifier, datatest); 
                precision.add(eval.precision(1));
            }
            catch(Exception e){
                //Eccezione sollevata se non ho sufficienti dati per elaborare modello
            }
            
        }

        return precision;
    }

    private static void splitDataset(ProjectInfo projectInfo) {
        Table dataset = Table.read().csv("DatasetMetrics"+projectInfo.getNameProject()+".csv");
        Table incrementalDataset = Table.create();
        double endperc=0.5;

        int size= projectInfo.getReleasesByPerc(endperc).size();
        int numTrainRelease =1;
        Table test;
        
        for(;numTrainRelease<size-2; numTrainRelease++){

            Release releaseTraining = projectInfo.getReleaseByNumber(numTrainRelease);
            Release releaseTesting1 = projectInfo.getReleaseByNumber(numTrainRelease+1);
            Release releaseTesting2 = projectInfo.getReleaseByNumber(numTrainRelease+2);

            Table partial = filterTable(dataset,releaseTraining.getName());
            
            if(incrementalDataset.isEmpty())
                incrementalDataset=partial;
            else
                incrementalDataset.append(partial);
            
            test= filterTable(dataset,releaseTesting1.getName());
            test.append(filterTable(dataset,releaseTesting2.getName()));
            
            incrementalDataset.write().csv("train"+numTrainRelease+".csv");
            test.write().csv("test"+numTrainRelease+".csv");
    
        }
            
    }

    public static Table filterTable(Table table , String release){
        return table.where(table.stringColumn(DatasetCreator.NameColumnDataset.RELEASE.toString()).isEqualTo(release));
         
    }

  

    

}

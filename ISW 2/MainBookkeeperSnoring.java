package com.fiscariello;

import java.io.File;

import java.util.ArrayList;

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

        String repoPath= "C:\\Users\\lucaf\\OneDrive\\Documenti\\GitHub\\bookkeeper\\.git";
        String projectName="BOOKKEEPER";
        String jiraTicket="BOOKKEEPER-";
        
        
        ProjectInfo projectInfo = new ProjectInfo(projectName, repoPath, jiraTicket);
        
        splitDataset(projectInfo);
        evalModel(projectInfo);
        

    }

    private static void evalModel(ProjectInfo projectInfo) throws Exception {

        double Endperc=0.5;
        int size= projectInfo.getReleasesByPerc(Endperc).size();
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
                System.out.println(eval.precision(1));
                precision.add(eval.precision(1));
            }
            catch(Exception e){
                e.printStackTrace();
            }
            
        }

        System.out.println(precision);

    }

    private static void splitDataset(ProjectInfo projectInfo) {
        Table dataset = Table.read().csv("DatasetMetrics.csv");
        Table incrementalDataset = Table.create();
        double Endperc=0.5;

        int size= projectInfo.getReleasesByPerc(Endperc).size();
        int numTrainRelease =1;
        
        for(;numTrainRelease<size-2; numTrainRelease++){

            Release releaseTraining = projectInfo.getReleaseByNumber(numTrainRelease);
            Release releaseTesting1 = projectInfo.getReleaseByNumber(numTrainRelease+1);
            Release releaseTesting2 = projectInfo.getReleaseByNumber(numTrainRelease+2);

            Table partial = filterTable(dataset,releaseTraining.getName());
            Table test = Table.create();
            
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
        Table datasetFiltr = table.where(
            table.stringColumn(DatasetCreator.NameColumnDataset.Release.toString()).isEqualTo(release));
        return datasetFiltr;
    }

  

    

}

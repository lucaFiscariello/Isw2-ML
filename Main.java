package com.fiscariello;

import java.util.ArrayList;
import java.util.List;

import com.fiscariello.ML.Weka;
import com.fiscariello.bug.Bug;
import com.fiscariello.bug.JiraHelper;
import com.fiscariello.bug.Proportion;
import com.fiscariello.dataset.Dataset;
import com.fiscariello.dataset.DatasetCreator;
import com.fiscariello.dataset.DatasetFinalCreator;
import com.fiscariello.project.ProjectInfo;
import com.fiscariello.project.Release;

import tech.tablesaw.api.Table;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;


public class Main {
    public static void main(String[] args) throws Exception {

        String repoPath= "C:\\Users\\lucaf\\OneDrive\\Documenti\\GitHub\\avro\\.git";
        String projectName="AVRO";
        String jiraTicket="AVRO-";
        double Endperc=0.5;
        double startperc=0.05;
        long pTest;
        long pTrain;

        String nameARFFTraining= "Training.arff";
        String nameARFFTesting = "Testing.arff";

        ProjectInfo projectInfo = new ProjectInfo(projectName, repoPath, jiraTicket);
        DatasetCreator datasetCreatorTraining= new DatasetCreator();
        DatasetFinalCreator finaldatasetCreator = new DatasetFinalCreator(600);
        Proportion proportion = new Proportion(projectInfo);
        JiraHelper jiraHelper= new JiraHelper(projectName);

        Weka weka = new Weka();
        Weka.OutputWeka output;
        ArrayList<Classifier> allClassifier = new ArrayList<>();
        allClassifier.add(new RandomForest());
        allClassifier.add(new NaiveBayes());
        allClassifier.add(new IBk());

        List<Bug> bugsTraining;
        List<Bug> bugsTesting;

        int size= projectInfo.getReleasesByPerc(Endperc).size();
        int numTrainRelease =(int) (size*startperc);

        //inizializzo training set
        for(Release release : projectInfo.getReleases(numTrainRelease-1))
            datasetCreatorTraining.addReleaseDataset(projectInfo,release.getName());

        //Realizzo training e testing set in parallelo
        for(;numTrainRelease<size-2; numTrainRelease++){
            DatasetCreator datasetCreatorTesting= new DatasetCreator();
            Release releaseTraining = projectInfo.getReleaseByNumber(numTrainRelease);
            Release releaseTesting1 = projectInfo.getReleaseByNumber(numTrainRelease+1);
            Release releaseTesting2 = projectInfo.getReleaseByNumber(numTrainRelease+2);

            datasetCreatorTraining.addReleaseDataset(projectInfo,releaseTraining.getName());
            datasetCreatorTesting.addReleaseDataset(projectInfo, releaseTesting1.getName());
            datasetCreatorTesting.addReleaseDataset(projectInfo, releaseTesting2.getName());

            proportion.setAllBug(jiraHelper.getAllBugPreviusRelease(releaseTesting2));
            pTest= proportion.getP_Training(releaseTesting2.getName());

            proportion.setAllBug(jiraHelper.getAllBugPreviusRelease(releaseTraining));
            pTrain= proportion.getP_Training(releaseTraining.getName());

            bugsTraining=jiraHelper.getAllBugPreviusRelease(releaseTraining);
            bugsTesting=jiraHelper.getAllBugPreviusRelease(releaseTesting2);

            //Scorro tutti i bug e cerco le classi buggy
            for (Bug bug : bugsTraining){

                //Aggiungo le classi buggy nel dataset
                List<String> buggyClass= jiraHelper.searchBuggyClass(projectInfo,bug.getDateFV(),bug.getKey());
                datasetCreatorTraining.addBuggyClassRational(buggyClass, bug.getaffectedRelease(),projectInfo);
                datasetCreatorTraining.addBuggyClassProportion(buggyClass, projectInfo, pTrain, bug);

            }

            for (Bug bug : bugsTesting){

                //Aggiungo le classi buggy nel dataset
                List<String> buggyClass= jiraHelper.searchBuggyClass(projectInfo,bug.getDateFV(),bug.getKey());
                datasetCreatorTesting.addBuggyClassRational(buggyClass, bug.getaffectedRelease(),projectInfo);
                datasetCreatorTesting.addBuggyClassProportion(buggyClass, projectInfo, pTest, bug);

            }

            Dataset datasetTraining= datasetCreatorTraining.geTable();
            datasetTraining.writeArff(nameARFFTraining);
            Dataset datasetTest= datasetCreatorTesting.geTable();
            datasetTest.writeArff(nameARFFTesting);


            try{
                double percTraintTotal = numTrainRelease/(double)size*100;
                int snoringClass = getTotalClassyBuggyByRelease(releaseTraining.getName())-datasetTraining.getNumberBuggyClassByRelease(releaseTraining.getName());
                
                for(Classifier classifier : allClassifier){
                    
                    output= weka.evalModel(nameARFFTraining, nameARFFTesting, classifier);
                    finaldatasetCreator.addRow(output, projectName, classifier,numTrainRelease,percTraintTotal,snoringClass);
    
                    output= weka.evalModelUnderSampling(nameARFFTraining, nameARFFTesting, classifier);
                    finaldatasetCreator.addRow(output, projectName, classifier,numTrainRelease,percTraintTotal,snoringClass);
    
                    output= weka.evalModelOverSampling(nameARFFTraining, nameARFFTesting, classifier);
                    finaldatasetCreator.addRow(output, projectName, classifier,numTrainRelease,percTraintTotal,snoringClass);
                        
                    output= weka.evalModelSMOTESampling(nameARFFTraining, nameARFFTesting, classifier);
                    finaldatasetCreator.addRow(output, projectName, classifier,numTrainRelease,percTraintTotal,snoringClass);
                }
            }
            catch(Exception e){
                System.err.println("Non ci sono dati sufficienti per valutare il modello");
            }
           
            
        }

        finaldatasetCreator.getTable().write().csv("FinalDatasetPresentation.csv");
        datasetCreatorTraining.geTable().write().csv("DatasetMetrics.csv");

    }


    private static int getTotalClassyBuggyByRelease(String release){
        Table datset = Table.read().csv("DatasetMetrics.csv");

        Table tableFilt = datset.where( 
            datset.stringColumn(DatasetCreator.NameColumnDataset.Release.toString()).isEqualTo(release)
            .and(datset.stringColumn(DatasetCreator.NameColumnDataset.Buggy.toString()).isEqualTo("YES")));

        return tableFilt.column(0).size();
    }



}

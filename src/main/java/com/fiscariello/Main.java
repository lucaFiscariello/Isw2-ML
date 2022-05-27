package com.fiscariello;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import com.fiscariello.bug.Bug;
import com.fiscariello.bug.JiraHelper;
import com.fiscariello.bug.Proportion;
import com.fiscariello.dataset.Dataset;
import com.fiscariello.dataset.DatasetCreator;
import com.fiscariello.progetto.ProjectInfo;
import com.fiscariello.progetto.Release;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONException;


public class Main {
    public static void main(String[] args) throws IOException, GitAPIException, JSONException, ParseException {

        String repoPath= "C:\\Users\\lucaf\\OneDrive\\Documenti\\GitHub\\avro\\.git";
        String projectName="AVRO";
        String jiraTicket="AVRO-";
        double Endperc=0.5;
        double startperc=0.1;
        long pTest;
        long pTrain;

        ProjectInfo projectInfo = new ProjectInfo(projectName, repoPath, jiraTicket);
        DatasetCreator datasetCreatorTraining= new DatasetCreator();
        DatasetCreator datasetCreatorTesting= new DatasetCreator();
        Proportion proportion = new Proportion(projectInfo);
        JiraHelper jiraHelper= new JiraHelper(projectName);

        List<Bug> bugsTraining;
        List<Bug> bugsTesting;

        int size= projectInfo.getReleasesByPerc(Endperc).size();
        int k =(int) (size*startperc);

        //inizializzo training set
        for(Release release : projectInfo.getReleases(k-1))
            datasetCreatorTraining.addReleaseDataset(projectInfo,release.getName());

        //Realizzo trining e testing set in parallelo
        for(;k<size-1; k++){

            Release releaseTraining = projectInfo.getReleaseByNumber(k);
            Release releaseTesting = projectInfo.getReleaseByNumber(k+1);

            datasetCreatorTraining.addReleaseDataset(projectInfo,releaseTraining.getName());
            datasetCreatorTesting.addReleaseDataset(projectInfo, releaseTesting.getName());

            proportion.setAllBug(jiraHelper.getAllBugPreviusRelease(releaseTesting));
            pTest= proportion.getP_Training(releaseTesting.getName());

            proportion.setAllBug(jiraHelper.getAllBugPreviusRelease(releaseTraining));
            pTrain= proportion.getP_Training(releaseTraining.getName());

            bugsTraining=jiraHelper.getAllBugPreviusRelease(releaseTraining);
            bugsTesting=jiraHelper.getAllBugPreviusRelease(releaseTesting);

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


        }


        Dataset datasetTraining= datasetCreatorTraining.geTable();
        datasetTraining.writeArff("Training.arff");
        Dataset datasetTest= datasetCreatorTesting.geTable();
        datasetTest.writeArff("Testing.arff");


    }



}

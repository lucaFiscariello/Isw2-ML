package com.fiscariello.bug;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fiscariello.project.ProjectInfo;
import com.fiscariello.project.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;

import org.eclipse.jgit.revwalk.RevCommit;

import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;


public class JiraHelper {

    private ArrayList<Bug> allBug;

    public JiraHelper(String projectName) throws IOException, JSONException{
        allBug=this.retrieve(projectName);
    }

    public ArrayList<Bug> getAllBug(){
        return this.allBug;
    }

    public List<Bug> getAllBugPreviusRelease(Release release){
        return this.allBug.stream().filter(b -> {
            try {
                return b.getDateOV().before(release.getFinalDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return false;
        }).collect(Collectors.toList());
    }


    public List<String> searchBuggyClass(ProjectInfo projectinfo, Date fixedRelease, String keyJira) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException, IncorrectObjectTypeException, IOException, JSONException, ParseException{

        Git git= projectinfo.getGit();
        List<String> buggyClass= new ArrayList<>();

        if(fixedRelease!= null){

            Release release= projectinfo.getReleaseByDate(fixedRelease);
            Iterable<RevCommit> log = git.log().add(release.getIdRelease()).call();

            //Filtro i commit i cui commenti contengono la stringa contenuta in keyJira
            Iterable<RevCommit> logGrep= StreamSupport.stream(log.spliterator(), false)
            .filter(x-> x.getFullMessage().contains(keyJira))
            .collect(Collectors.toList());

            //Scorro tutti i commit filtrati e trovo i file modificati per quel commit
            Iterator<RevCommit> iterator= logGrep.iterator();
            while(iterator.hasNext()){
                ObjectId oldHash =iterator.next().getId();

                ObjectId newCommit = git.getRepository().resolve(oldHash.getName()+"^{tree}");
                ObjectId oldCommit = git.getRepository().resolve(oldHash.getName()+"~1^{tree}");

                ObjectReader reader = git.getRepository().newObjectReader();
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset( reader, oldCommit );
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset( reader,newCommit );

                List<DiffEntry> diffs = git.diff()
                .setOldTree(oldTreeIter)
                .setNewTree(newTreeIter)
                .call();

                for (DiffEntry diff : diffs) {
                    String diftype=diff.getChangeType().toString();
                    String pathFile= diff.getNewPath();

                    if(diftype.equals("MODIFY") & pathFile.endsWith(".java") & !pathFile.contains("test")){
                        String pathFileJava = projectinfo.getRelativePathFile(pathFile);
                        buggyClass.add(pathFileJava);
                    }

                }
            }

        }

        return buggyClass;

    }



    private ArrayList<Bug> retrieve(String projName) throws IOException, JSONException {

        Integer j = 0, i = 0, total = 1;

        JSONArray totalIssues=new JSONArray();
        ArrayList<Bug> allBug = new ArrayList<>();

        //Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
              + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
              + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,fixVersions,resolutiondate,versions,created&startAt="
              + i.toString() + "&maxResults=" + j.toString();

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");


            total = json.getInt("total");

            for (; i < total && i < j; i++)
                //Iterate through each bug
                totalIssues.put(issues.getJSONObject(i%1000));


        } while (i < total);

        for (int k = 0; k < totalIssues.length(); k++) {
            Bug bug = new Bug(totalIssues.getJSONObject(k));
            allBug.add(bug);
        }
        return allBug;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
           sb.append((char) cp);
        }
        return sb.toString();
     }

    private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }



}

package com.fiscariello.project;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.ibm.icu.text.SimpleDateFormat;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class ProjectInfo {

    private String nameProject;
    private String pathRepository;
    private String jiraTicket;
    private ArrayList<Release> release;
    private Git git;

    public ProjectInfo(String namePoject, String pathRepository,String jiraTicket) throws IOException, GitAPIException, ParseException{
        this.nameProject=namePoject;
        this.pathRepository=pathRepository;
        this.jiraTicket=jiraTicket;

        //Inizializzo repository git
        Repository repo = new FileRepositoryBuilder().setGitDir(new File(pathRepository)).build();
        this.git = new Git(repo);

        //ottengo la liste di tutte le release del progetto
        List<Ref> allRelease= git.tagList().call();

        Ref releaseCorr;
        String nameRelease;
        Iterable<RevCommit> commits ;
        HashSet<Release> releaseUnique = new HashSet<>();
        Date endRelease=null;
        ObjectId idRelease;

        //memorizzo per ogni release il nome, la data di pubblicazione e tutti i suoi commmit
        for(int i=0; i<allRelease.size(); i++){
            try{
                releaseCorr=allRelease.get(i);
                nameRelease=releaseCorr.getName();
                idRelease= releaseCorr.getPeeledObjectId();

                commits = git.log().add(idRelease).call();

                RevCommit lastCommit=commits.iterator().next();
                endRelease=lastCommit.getAuthorIdent().getWhen();

                //aggiungo in un Hashset le release. In questo modo  considero una sola volta le release con le stesse date di pubblicazione
                Release r = new Release( endRelease, commits,nameRelease,idRelease);
                releaseUnique.add(r);

            }
            catch(NullPointerException e){
                //la release refs/tags/release-1.9.0 presenta un bug che fa fallire un metodo interno di Jgit
            }
        }

        //Ordino le release
        this.release = new ArrayList<>(releaseUnique);
        this.release.sort((Release r1, Release r2) -> r1.getFinalDate().compareTo(r2.getFinalDate()));

        Release r1= this.release.get(0);
        Release r2;

        r1.setInitialDate(new Date(0));

        //aggiorno le date di inizio di tutte le release
        for (int i=0; i<this.release.size()-1; i++) {
            r1= this.release.get(i);
            r2= this.release.get(i+1);

            r2.setInitialDate(r1.getFinalDate());
        }


    }

    public String getNameProject(){
        return this.nameProject;
    }

    public String getJiraTicket(){
        return this.jiraTicket;
    }

    public String getPathRepository(){
        return this.pathRepository;
    }

    public String getrootRepository(){
        return this.pathRepository.replace(".git", "");
    }

    public List<Release> getReleasesByPerc(double perc){
        return  this.release.subList(0, (int)(this.release.size()*perc));
    }

    public List<Release> getReleases(int releaseNumber){
        return  this.release.subList(0, releaseNumber);
    }

    public Release getReleaseByNumber(int releaseNumber){
        return  this.release.get(releaseNumber);
    }

    public List<Release> getRelease(){
        return  this.release;
    }

    public Git getGit(){
        return this.git;
    }

    public String getRelativePathFile(String file) {
        String rootPath=this.getrootRepository();
        return file.replace(rootPath, "").replace("\\", "/");
    }

    public Release getReleaseByName(String tag) {

        for (Release r : this.release) {
            if(r.getName().equals(tag))
                return r;
        }

        return null;
    }

    public String getNameFileByPath(String javaPathClass) {
        return javaPathClass.substring(javaPathClass.lastIndexOf("\\")+1, javaPathClass.length());
    }


    public Release getReleaseByDate(String datastr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
        Date data= sdf.parse(datastr);

        for (Release r : release) {
            if(r.isBetween(data))
                return r;
        }

        return null;

    }

    public Release getReleaseByDate(Date data) throws ParseException {
        for (Release r : release) {
            if(r.isBetween(data))
                return r;
        }

        return null;

    }

    public ArrayList<File> getClassesByReleaseTag(String tag) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException{

        //Cambio release
        this.git.checkout().setName(tag).call();

        //Lista dei file.
        File[] initialFile= git.getRepository().getWorkTree().listFiles();
        Queue<File> allFile = new LinkedList<>(Arrays.asList(initialFile));

        ArrayList<File> javaClasses = new ArrayList<>();
        File file=null;

        //Scorro iterativamente le cartelle per trovare i file java
        while(!allFile.isEmpty()){
            file=allFile.remove();
            String nameFile=file.getName();
            String pathFile = file.getPath();
            Boolean isDirectory = file.isDirectory();

            if(!isDirectory & nameFile.endsWith(".java") &  !pathFile.contains("test") ){
                javaClasses.add(file);
            }

            if(isDirectory){
                Collection<File> newFile =  Arrays.asList(file.listFiles());
                allFile.addAll(newFile);
            }
        }
        return javaClasses;
    }






}

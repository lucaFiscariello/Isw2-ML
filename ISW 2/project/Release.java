package com.fiscariello.project;

import java.text.ParseException;
import java.util.Date;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

public class Release{
    private String name;
    private Date initialData;
    private Date finalData;
    private Iterable<RevCommit> commits;
    private ObjectId idRelease;

    public Release(Date finalData,Iterable<RevCommit> commits,String name,ObjectId idRelease) throws ParseException{
        this.finalData=finalData;
        this.commits=commits;
        this.name=name;
        this.idRelease=idRelease;
    }


    public boolean isBetween(Date data) throws ParseException{
        return data.after(initialData) & data.before(finalData);
    }

    public Iterable<RevCommit> getCommits(){
        return this.commits;
    }

    public Date getInitialDate(){
        return this.initialData;
    }

    public Date getFinalDate(){
        return this.finalData;
    }

    public String getName(){
        return this.name;
    }

    public ObjectId getIdRelease(){
        return this.idRelease;
    }

    public void setInitialDate(Date data){
        this.initialData=data;
    }

    public int hashCode(){
        return this.finalData.hashCode();
    }


    public boolean equals(Object release){
        Release r =(Release) release;
        return r.getFinalDate().equals(this.finalData);
    }

}


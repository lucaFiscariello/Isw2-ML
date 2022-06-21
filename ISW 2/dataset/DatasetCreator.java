package com.fiscariello.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.fiscariello.project.ProjectInfo;
import com.fiscariello.project.Release;
import com.fiscariello.bug.Bug;
import com.google.common.collect.Lists;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.Edit.Type;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.json.JSONArray;
import org.json.JSONException;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;


public class DatasetCreator {
    private Dataset table;

    public enum NameColumnDataset {
        Revision,Author_Number,Bug_Fix,Churn,Path_classe,Release,Buggy,Loc,Weigthed_Age,Age_class,
        Loc_Added, ChgSetSize, Loc_Touched, Loc_Weighted_Methods, Variance_Loc_Added;
    }

    public DatasetCreator(){
        this.table=null;
    }

    public Dataset geTable(){
        return this.table;
    }

    public void setTable(Dataset table){
        this.table=table;
    }

    public void concatenateDataset(Dataset dataset){
        if(this.table==null)
            table=dataset;
        else
            table.append(dataset);
    }

    public void addReleaseDataset(ProjectInfo projectInfo, String tag )throws IOException, GitAPIException {

        Git git = projectInfo.getGit();
        HashSet<PersonIdent> authorName = new HashSet<>();

        //Ottengo le classe della release corrente
        ArrayList<File> javaClasses=projectInfo.getClassesByReleaseTag(tag);
        ArrayList<String> javaPathClasses = new ArrayList<>();
        Release release = projectInfo.getReleaseByName(tag);

        //Inizializzo variabili per creare colonne
        int size=javaClasses.size();
        Integer[] revisionarray = new Integer[size];
        Integer[] authorNumer = new Integer[size];
        Integer[] bugFix = new Integer[size];
        Integer[] churnArray = new Integer[size];
        Integer[] locAdded = new Integer[size];
        Integer[] chgSetSizeArray = new Integer[size];
        Integer[] locTouched = new Integer[size];
        Integer[] locClasses = new Integer[size];
        Double[] locWmethods = new Double[size];
        Double[] varLinesAdded = new Double[size];

        //Scorro tutte le classi
        for(int i=0; i<size;i++){

            String javaPathclass= javaClasses.get(i).getPath();
            String relativePath = projectInfo.getRelativePathFile(javaPathclass);
            File singleClass=javaClasses.get(i);

            javaPathClasses.add(relativePath);

            //Ottengo tutti i commit della classe i-esima fino a una release specifica
            Iterable<RevCommit> commits = git.log().addPath(relativePath).add(release.getIdRelease()).call();
            List<RevCommit> commitList = Lists.newArrayList(commits.iterator());

            //Variabili usate per il calcolo delle metriche
            ArrayList<Integer> varianceLinesAdd = new ArrayList<>();
            String javaName;
            int countbugFix=0;
            int linesDeleted=0;
            int linesAdded=0;
            int linesAddedPartial;
            int linesModified=0;
            int chgSetSize=0;
            int loc=0;
            int numberMethods=0;

            for(RevCommit commit: commitList){

                //Trovo il numero di autori per una classe specifica.
                authorName.add(commit.getAuthorIdent());

                //Trovo i bugFix per quella classe.
                if(commit.getFullMessage().contains(projectInfo.getJiraTicket()))
                    countbugFix++;

                //Confronto ogni commit della lista con il suo precedente
                String commitid= commit.getName();
                ObjectId newCommit = git.getRepository().resolve(commitid+"^{tree}");
                ObjectId oldCommit = git.getRepository().resolve(commitid+"~1^{tree}");

                ObjectReader reader = git.getRepository().newObjectReader();
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset( reader, oldCommit );
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset( reader,newCommit );

                DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
                diffFormatter.setRepository( git.getRepository() );
                diffFormatter.setContext( 0 );
                List<DiffEntry> entries = diffFormatter.scan( newTreeIter, oldTreeIter );

                //Scorro tutti i file modificati in un dato commit e caolcolo le metriche lineTouched, churn , varLinesAdded e chgSetSize
                for( DiffEntry entry : entries ) {
                    javaName = projectInfo.getNameFileByPath(javaPathclass);
                    chgSetSize++;

                    if(entry.getNewPath().contains(javaName)){
                        for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
                            linesAddedPartial=edit.getEndB() - edit.getBeginB();
                            linesDeleted += edit.getEndA() - edit.getBeginA();
                            linesAdded += linesAddedPartial;
                            linesModified+=(edit.getType().equals(Type.REPLACE))? 1:0;

                            varianceLinesAdd.add(linesAddedPartial);
                        }
                    }
                }

            }


            //Calcolo le metriche loc e locWmethods
            try (BufferedReader reader = new BufferedReader(new FileReader(singleClass))) {

                String line = reader.readLine();

                while(line!=null) {
                    if(line.isBlank()){
                        loc++;
                        boolean isMethod=   line.contains("(") & line.contains(")") & !line.contains(" class ")&
                                            !line.contains("if(") & !line.contains("for(") & !line.contains("do(") &
                                            !line.contains("while(") & !line.contains("try(") & !line.contains(";")&
                                            !line.contains("if (") & !line.contains("for (") & !line.contains("do (") &
                                            !line.contains("while (") & !line.contains("try (");
                        if(isMethod)
                            numberMethods++;
                    }

                    line = reader.readLine();
                }


            }

            authorNumer[i]=authorName.size();
            revisionarray[i]=commitList.size();
            bugFix[i]=countbugFix;
            churnArray[i]=linesAdded-linesDeleted;
            locAdded[i]=linesAdded;
            chgSetSizeArray[i]=chgSetSize;
            locTouched[i]=linesAdded+linesDeleted+linesModified;
            varLinesAdded[i]=Statistics.getVariance(varianceLinesAdd);
            numberMethods=(numberMethods>0)? numberMethods:1;
            locClasses[i]=loc;
            locWmethods[i]=(double) (loc/numberMethods);

        }


        IntColumn revision = IntColumn.create(NameColumnDataset.Revision.toString(), revisionarray);
        IntColumn author = IntColumn.create(NameColumnDataset.Author_Number.toString(), authorNumer);
        IntColumn bugFixColumn = IntColumn.create(NameColumnDataset.Bug_Fix.toString(), bugFix);
        IntColumn churnColumn= IntColumn.create(NameColumnDataset.Churn.toString(), churnArray);
        IntColumn locAddedColumn= IntColumn.create(NameColumnDataset.Loc_Added.toString(), locAdded);
        IntColumn chgSetSizeColumn= IntColumn.create(NameColumnDataset.ChgSetSize.toString(), chgSetSizeArray);
        IntColumn locTouchedColumn= IntColumn.create(NameColumnDataset.Loc_Touched.toString(), locTouched);
        IntColumn loc= IntColumn.create(NameColumnDataset.Loc.toString(), locClasses);
        IntColumn ageClass = createAgeClassColumn(tag,projectInfo,javaPathClasses);

        DoubleColumn locWmethodsColumn= DoubleColumn.create(NameColumnDataset.Loc_Weighted_Methods.toString(), locWmethods);
        DoubleColumn varianceLocAddColumn= DoubleColumn.create(NameColumnDataset.Variance_Loc_Added.toString(), varLinesAdded);

        StringColumn pathClass =  StringColumn.create(NameColumnDataset.Path_classe.toString(),javaPathClasses);
        StringColumn releaseColumn = StringColumn.create(NameColumnDataset.Release.toString(),Collections.nCopies(pathClass.size(), tag));
        StringColumn buggy = StringColumn.create(NameColumnDataset.Buggy.toString(),Collections.nCopies(pathClass.size(), "NO"));

        Dataset dataset = new Dataset("Dataset");
        dataset.addColumns(pathClass,releaseColumn,loc,revision,author,bugFixColumn,ageClass,churnColumn,locAddedColumn,chgSetSizeColumn,locTouchedColumn,locWmethodsColumn,varianceLocAddColumn,buggy);


        //Concateno dataset appena creato
        this.concatenateDataset(dataset);

    }


    private static IntColumn createAgeClassColumn(String tag, ProjectInfo projectinfo, ArrayList<String> javaPathClasses) throws NoHeadException, GitAPIException, MissingObjectException, IncorrectObjectTypeException {
        Git git= projectinfo.getGit();
        Release release= projectinfo.getReleaseByName(tag);

        int size=javaPathClasses.size();
        Integer[] ageClass = new Integer[size];

        //Scorro tutte le classi
        for(int i=0; i<size;i++){

            //per ogni classe isolo relative path e ottengo i commit in cui compare quella classe
            String relativePath = projectinfo.getRelativePathFile(javaPathClasses.get(i));
            Iterable<RevCommit> commits = git.log().addPath(relativePath).add(release.getIdRelease()).call();

            RevCommit firstCommit= commits.iterator().next();
            
            if(firstCommit != null){
                //in secondi
                int age= firstCommit.getCommitTime();

                //in minuti
                age=age/60;

                //in ore
                age=age/60;

                //in giorni
                age=age/24;

                //in settimane
                age=age/7;

                ageClass[i]=age;
            }
            else{
                ageClass[i]=0;
            }
            
            

        }

        return IntColumn.create(NameColumnDataset.Age_class.toString(), ageClass);
    }

    public void addBuggyClassRational(List<String> buggyClass , JSONArray affectedRelease, ProjectInfo projectinfo) throws JSONException, ParseException{
        String affRelData;
        Release release;
        String releaseName;

        //Scorro tutte le classi buggy
        for(String nameClass :buggyClass ){
            for(int i =0 ; i<affectedRelease.length() ; i++){

                if(affectedRelease.getJSONObject(i).has("releaseDate")){

                    affRelData= affectedRelease.getJSONObject(i).getString("releaseDate");
                    release=projectinfo.getReleaseByDate(affRelData);
                    releaseName=release.getName();

                    Iterator<Row> iterator = this.table.iterator();
                    Row row= iterator.next();
                    Boolean loop=true;

                    //Scorro ogni riga del dataset
                    while(iterator.hasNext() & loop){

                        Boolean conditionrelease= row.getString(NameColumnDataset.Release.toString()).equals(releaseName);
                        Boolean conditionglobal= row.getString(NameColumnDataset.Path_classe.toString()).equals(nameClass) & conditionrelease;

                        //Trovo classe con nome e release di interesse
                        if(conditionglobal){
                            row.setString(NameColumnDataset.Buggy.toString(), "YES");
                            loop=false;
                        }

                        row= iterator.next();

                        //Introduco questa condizione per non scorrere tutte le righe.
                        if(conditionrelease)
                            loop=true;
                    }
                }

            }
        }

    }

    public void addBuggyClassProportion(List<String> buggyClass , ProjectInfo projectinfo, long p, Bug bug) throws JSONException, ParseException{

        //Controllo se il bug non ha un IV
        if(bug.getDateIV()==null & bug.getDateFV()!=null){
            long fv=bug.getDateFV().getTime();
            long ov=bug.getDateOV().getTime();
            long iv=fv-(fv-ov)*p;

            //Se il bug non ha IV la calcolo utilizzando proportional
            Date dataIV = new Date(iv);

            //Scorro le classi buggy
            for(String nameClass :buggyClass ){
                Iterator<Row> iterator = this.table.iterator();
                Row row= iterator.next();
                Boolean loop=true;

                while(iterator.hasNext() & loop){
                    Release currentRelease = projectinfo.getReleaseByName(row.getString(NameColumnDataset.Release.toString()));
                    Date currentReleaseDate= currentRelease.getFinalDate();

                    Boolean conditionrelease= currentReleaseDate.before(bug.getDateFV()) & currentReleaseDate.after(dataIV) ;
                    Boolean conditionglobal= row.getString(NameColumnDataset.Path_classe.toString()).equals(nameClass) & conditionrelease;

                    //Setto la classe buggy in tutte le release comprese tra IV e FV esclusa
                    if(conditionglobal){
                        row.setString(NameColumnDataset.Buggy.toString(), "YES");
                        loop=false;
                    }

                    row= iterator.next();

                    //Introduco questa condizione per non scorrere tutte le righe.
                    if(conditionrelease)
                        loop=true;
                }
        }



    }

}


public static class Statistics {

    static double getMean(ArrayList<Integer> data) {
        double sum = 0.0;
        for(double a : data)
            sum += a;

        return sum/data.size();
    }

    static double getVariance(ArrayList<Integer> varianceLinesAdd) {
        double mean = getMean(varianceLinesAdd);
        double temp = 0;

        for(double a :varianceLinesAdd)
            temp += (a-mean)*(a-mean);

        return temp/(varianceLinesAdd.size());
    }
}

}

package com.fiscariello.bug;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ibm.icu.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Bug {
    private String key;
    private String dataCreationTicket;
    private JSONArray affectedRelease;
    private JSONArray fixedRelease;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");

    private String releaseDate= "releaseDate";
    private String keystr = "key";
    private String fields = "fields";
    private String versions = "versions";
    private String fixVersions = "fixVersions";
    private String created = "created";

    
    public Bug(JSONObject singleIussue) throws JSONException{

        this.key= singleIussue.getString(keystr);
        this.affectedRelease = singleIussue.getJSONObject(fields).getJSONArray(versions);
        this.fixedRelease= singleIussue.getJSONObject(fields).getJSONArray(fixVersions);
        this.dataCreationTicket= singleIussue.getJSONObject(fields).getString(created).substring(0,10);
    }

    public String getKey(){
        return this.key;
    }

    public String getDateCreationTicket(){
        return this.dataCreationTicket;
    }

    public JSONArray getaffectedRelease(){
        return this.affectedRelease;
    }

    public JSONArray getfixedRelease(){
        return this.fixedRelease;
    }

    public List<String> getFixedReleaseDate() throws JSONException{

        
        ArrayList<String> releaseDateFix= new ArrayList<>();
        for( int i=0 ;i<fixedRelease.length(); i++){

            if(fixedRelease.getJSONObject(i).has(releaseDate)){
                releaseDateFix.add(fixedRelease.getJSONObject(i).getString(releaseDate));
            }

        }
        return releaseDateFix;
    }

    public Date getDateIV() throws JSONException, ParseException{
        if(affectedRelease.length()>0 && affectedRelease.getJSONObject(0).has(releaseDate)){
             return sdf.parse(affectedRelease.getJSONObject(0).getString(releaseDate));
        }
        return null;
    }

    public Date getDateFV() throws JSONException, ParseException{
        if(fixedRelease.length()>0 && fixedRelease.getJSONObject(0).has(releaseDate)){
            return sdf.parse(fixedRelease.getJSONObject(0).getString(releaseDate));
        }
        return null;
    }

    public Date getDateOV() throws ParseException{
        return sdf.parse(this.dataCreationTicket);
    }


    public boolean isValid() throws ParseException, JSONException{

        if(getDateOV()!= null && getDateIV()!= null && getDateFV()!= null ){

            Date ov= getDateOV();
            Date iv= getDateIV();
            Date fv= getDateFV();

            return fv.after(ov) && (ov.after(iv) || ov.equals(iv)) ;
        }

        return false;

    }

}

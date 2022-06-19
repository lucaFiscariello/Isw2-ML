package com.fiscariello.bug;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.fiscariello.project.ProjectInfo;
import com.fiscariello.project.Release;
import org.json.JSONException;

public class Proportion {

    private List<Bug> allBug;
    private ProjectInfo projectInfo;

    public Proportion(ArrayList<Bug> allBug,ProjectInfo projectInfo){
        this.allBug=allBug;
        this.projectInfo=projectInfo;
    }

    public Proportion(ProjectInfo projectInfo){
        this.projectInfo=projectInfo;
    }

    public void setAllBug(List<Bug> list){
        this.allBug=list;
    }

    public long getP_Training(String endRelease) throws ParseException, JSONException{

        Release release =projectInfo.getReleaseByName(endRelease);

        long fv;
        long iv;
        long ov;
        long p;

        long count=0;
        long summ=0;

        for (Bug bug : allBug) {
            if(bug.isValid() )
                if(bug.getDateFV().before(release.getFinalDate())){
                    fv=bug.getDateFV().getTime();
                    ov=bug.getDateIV().getTime();
                    iv=bug.getDateOV().getTime();

                    if(fv-ov>0 ){

                        p=(fv-iv)/(fv-ov);

                        if(p>0){
                            summ=summ+p;
                            count++;
                        }

                }
            }
        }

        if(count==0)
            return 0;

        return summ/count;
    }


}

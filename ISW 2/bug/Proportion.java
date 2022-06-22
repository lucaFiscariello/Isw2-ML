package com.fiscariello.bug;

import java.text.ParseException;
import java.util.List;

import com.fiscariello.project.ProjectInfo;
import com.fiscariello.project.Release;
import org.json.JSONException;

public class Proportion {

    private List<Bug> allBug;
    private ProjectInfo projectInfo;

    public Proportion(List<Bug> allBug,ProjectInfo projectInfo){
        this.allBug=allBug;
        this.projectInfo=projectInfo;
    }

    public Proportion(ProjectInfo projectInfo){
        this.projectInfo=projectInfo;
    }

    public void setAllBug(List<Bug> list){
        this.allBug=list;
    }

    public long getPTraining(String endRelease) throws ParseException, JSONException{

        Release release =projectInfo.getReleaseByName(endRelease);

        long fv=0;
        long iv=0;
        long ov=0;
        long p;

        long count=0;
        long summ=0;

        for (Bug bug : allBug) {
            if(bug.isValid() && bug.getDateFV().before(release.getFinalDate())){
                    
                fv=bug.getDateFV().getTime();
                ov=bug.getDateIV().getTime();
                iv=bug.getDateOV().getTime();

                p=(fv-iv)/(fv-ov);

                if(p>0){
                    summ=summ+p;
                    count++;
                }

            }
            
        }

        if(count==0)
            return 0;

        return summ/count;
    }


}

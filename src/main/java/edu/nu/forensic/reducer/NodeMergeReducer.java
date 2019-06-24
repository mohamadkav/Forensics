package edu.nu.forensic.reducer;


import edu.nu.forensic.db.entity.IoEventAfterCPR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class NodeMergeReducer {

    private Set<String> fileslists;

    private List<String> judgeProcessId=new ArrayList<>();
    @Autowired
    public NodeMergeReducer(){
        fileslists=null;//getfilelist();
    }
    private boolean reduce(IoEventAfterCPR event){
        try {
            try{
                if(event.getNames().contains("FileIoRead")) {
                    if (fileslists.contains(event.getPredicateObjectPath())) {
                        if (!judgeProcessId.contains(event.getSubjectUUID().toString())) {
                            judgeProcessId.add(event.getSubjectUUID().toString());
                            event.setNames("Init Process");
                            return true;
                        } //else bufferedWriter.append(event.getNames()+"\r\n");
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //False return type means that we have to store the event
    public boolean JsonReduce(IoEventAfterCPR event){
        try{
            return reduce(event);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}

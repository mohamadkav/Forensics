package edu.nu.forensic.reducer;


import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.IoEventAfterCPR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class NodeMergeReducer {

    private Set<String> fileslists=new HashSet<>();

    private List<String> judgeProcessId=new ArrayList<>();

    public boolean reduceOnlyGetTempFile(IoEventAfterCPR event){
        try {
            try{
                if(event.getNames().contains("FileIoRead")) {
                    if (!fileslists.contains(event.getPredicateObjectPath())) {
                        if (!judgeProcessId.contains(event.getSubjectUUID().toString())) {
                            judgeProcessId.add(event.getSubjectUUID().toString());
                            event.setNames("Init Process");
                            return true;
                        }
                    }
                }
                else if(event.getNames().contains("FileIoWrite")){
                    fileslists.add(event.getPredicateObjectPath());
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
    public boolean reduce(IoEventAfterCPR event){
        try{
            return reduceOnlyGetTempFile(event);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}

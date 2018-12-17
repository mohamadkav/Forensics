package edu.nu.forensic.util;


import edu.nu.forensic.db.entity.Principal;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.db.repository.PrincipalRepository;
import edu.nu.forensic.db.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RecordConverter {
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private PrincipalRepository principalRepository;
    public Subject saveAndConvertBBNSubjectToSubject(com.bbn.tc.schema.avro.cdm19.Subject bbnSubject){
        Subject parentSubject=null;
        Principal localPrincipal=null;
        if(bbnSubject.getParentSubject()!=null)
            parentSubject=subjectRepository.findById(UUID.nameUUIDFromBytes(bbnSubject.getParentSubject().bytes())).orElse(null);
        if(bbnSubject.getLocalPrincipal()!=null)
            localPrincipal=principalRepository.findById(UUID.nameUUIDFromBytes(bbnSubject.getLocalPrincipal().bytes())).orElse(null);
        Subject subject=new Subject(UUID.nameUUIDFromBytes(bbnSubject.getUuid().bytes()),bbnSubject.getType().name(),
                bbnSubject.getCid(),parentSubject,localPrincipal,bbnSubject.getStartTimestampNanos(),
                bbnSubject.getCmdLine()==null?null:bbnSubject.getCmdLine().toString(),bbnSubject.getPrivilegeLevel()==null?null:bbnSubject.getPrivilegeLevel().name());
        return subjectRepository.save(subject);
    }

    public Principal saveAndConvertBBNPrincipalToPrincipal(com.bbn.tc.schema.avro.cdm19.Principal bbnPrincipal){
        StringBuilder groupIds= new StringBuilder();
        for(CharSequence cs:bbnPrincipal.getGroupIds())
            groupIds.append(cs.toString()).append(",");
        if(!groupIds.toString().isEmpty())
            groupIds.deleteCharAt(groupIds.length() - 1);
        else
            groupIds=null;
        Principal principal=new Principal(UUID.nameUUIDFromBytes(bbnPrincipal.getUuid().bytes()),bbnPrincipal.getType().name(),bbnPrincipal.getUserId()==null?null:
                bbnPrincipal.getUserId().toString(),
                bbnPrincipal.getUsername()==null?null:bbnPrincipal.getUsername().toString(),groupIds==null?null:groupIds.toString());
        return principalRepository.save(principal);
    }
}

package edu.nu.forensic.util;


import edu.nu.forensic.db.entity.Object;
import edu.nu.forensic.db.entity.*;
import edu.nu.forensic.db.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RecordConverter {
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private PrincipalRepository principalRepository;
    @Autowired
    private NetFlowObjectRepository netFlowObjectRepository;
    @Autowired
    private FileObjectRepository fileObjectRepository;
    @Autowired
    private RegistryKeyObjectRepository registryKeyObjectRepository;
    @Autowired
    private ObjectRepository objectRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UnitDependencyRepository unitDependencyRepository;
    public Subject saveAndConvertBBNSubjectToSubject(com.bbn.tc.schema.avro.cdm19.Subject bbnSubject){
        Subject parentSubject=null;
        Principal localPrincipal=null;
        if(bbnSubject.getParentSubject()!=null)
            parentSubject=subjectRepository.findById(UUID.nameUUIDFromBytes(bbnSubject.getParentSubject().bytes())).orElse(null);
        if(bbnSubject.getLocalPrincipal()!=null)
            localPrincipal=principalRepository.findById(UUID.nameUUIDFromBytes(bbnSubject.getLocalPrincipal().bytes())).orElse(null);
        Subject subject=new Subject(UUID.nameUUIDFromBytes(bbnSubject.getUuid().bytes()),bbnSubject.getType().name(),
                bbnSubject.getCid(),parentSubject==null?null:parentSubject.getUuid(),localPrincipal,bbnSubject.getStartTimestampNanos(),
                bbnSubject.getCmdLine()==null?null:bbnSubject.getCmdLine().toString(),bbnSubject.getPrivilegeLevel()==null?null:bbnSubject.getPrivilegeLevel().name());
        return subjectRepository.save(subject);
    }

    public Principal saveAndConvertBBNPrincipalToPrincipal(com.bbn.tc.schema.avro.cdm19.Principal bbnPrincipal){
        StringBuilder groupIds= new StringBuilder();
        if(bbnPrincipal.getGroupIds()!=null)
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

    public RegistryKeyObject saveAndConvertBBNRegistryKeyObjectToRegistryKeyObject(com.bbn.tc.schema.avro.cdm19.RegistryKeyObject bbnRegistryKeyObject){
        RegistryKeyObject registryKeyObject=new RegistryKeyObject(UUID.nameUUIDFromBytes(bbnRegistryKeyObject.getUuid().bytes()),bbnRegistryKeyObject.getSize(),
                bbnRegistryKeyObject.getKey()!=null?bbnRegistryKeyObject.getKey().toString():null);
        return registryKeyObjectRepository.save(registryKeyObject);
    }
    public FileObject saveAndConvertBBNFileObjectToFileObject(com.bbn.tc.schema.avro.cdm19.FileObject bbnFileObject){
        Principal localPrincipal=null;
        if(bbnFileObject.getLocalPrincipal()!=null)
            localPrincipal=principalRepository.findById(UUID.nameUUIDFromBytes(bbnFileObject.getLocalPrincipal().bytes())).orElse(null);
        FileObject fileObject=new FileObject(UUID.nameUUIDFromBytes(bbnFileObject.getUuid().bytes()),bbnFileObject.getFileDescriptor(),
                localPrincipal,bbnFileObject.getSize());
        return fileObjectRepository.save(fileObject);
    }
    public NetFlowObject saveAndConvertBBNNetFlowObjectToNetFlowObject(com.bbn.tc.schema.avro.cdm19.NetFlowObject bbnNetFlowObject){
        NetFlowObject netFlowObject=new NetFlowObject(UUID.nameUUIDFromBytes(bbnNetFlowObject.getUuid().bytes()),bbnNetFlowObject.getLocalAddress()!=null?bbnNetFlowObject.getLocalAddress().toString():
                null,bbnNetFlowObject.getLocalPort(),bbnNetFlowObject.getRemoteAddress()!=null?bbnNetFlowObject.getRemoteAddress().toString():null,bbnNetFlowObject.getRemotePort(),
                bbnNetFlowObject.getIpProtocol());
        return netFlowObjectRepository.save(netFlowObject);
    }
    public Event saveAndConvertBBNEventToEvent(com.bbn.tc.schema.avro.cdm19.Event bbnEvent){
        Subject subject=null;
        if(bbnEvent.getSubject()!=null)
            subject=subjectRepository.findById(UUID.nameUUIDFromBytes(bbnEvent.getSubject().bytes())).orElse(null);
        Object predicateObject=null,predicateObject2=null;
        if(bbnEvent.getPredicateObject()!=null)
            predicateObject=objectRepository.findById(UUID.nameUUIDFromBytes(bbnEvent.getPredicateObject().bytes())).orElse(null);
        if(bbnEvent.getPredicateObject2()!=null)
            predicateObject2=objectRepository.findById(UUID.nameUUIDFromBytes(bbnEvent.getPredicateObject2().bytes())).orElse(null);
        StringBuilder eventNames= new StringBuilder();
        if(bbnEvent.getNames()!=null) {
            for (CharSequence cs : bbnEvent.getNames())
                eventNames.append(cs.toString()).append(",");
        }
        if(!eventNames.toString().isEmpty())
            eventNames.deleteCharAt(eventNames.length() - 1);
        else
            eventNames=null;
        Event event=new Event(UUID.nameUUIDFromBytes(bbnEvent.getUuid().bytes()),bbnEvent.getType()==null?null:bbnEvent.getType().name(),
                bbnEvent.getThreadId(),subject==null?null:subject.getUuid(),bbnEvent.getPredicateObjectPath()!=null?bbnEvent.getPredicateObjectPath().toString():null,
                bbnEvent.getTimestampNanos(),eventNames!=null?eventNames.toString():null,false);
//        Event event=new Event(UUID.nameUUIDFromBytes(bbnEvent.getUuid().bytes()),bbnEvent.getSequence(),bbnEvent.getType()==null?null:bbnEvent.getType().name(),
//                bbnEvent.getThreadId(),subject,predicateObject,bbnEvent.getPredicateObjectPath()!=null?bbnEvent.getPredicateObjectPath().toString():null,
//                predicateObject2,bbnEvent.getPredicateObject2Path()!=null?bbnEvent.getPredicateObject2Path().toString():null,bbnEvent.getTimestampNanos(),
//                eventNames!=null?eventNames.toString():null,bbnEvent.getLocation(),bbnEvent.getSize(),bbnEvent.getProgramPoint()!=null?bbnEvent.getProgramPoint().toString():null);
        return eventRepository.save(event);
    }
    public UnitDependency saveAndConvertBBNUnitDependencyToUnitDependency(com.bbn.tc.schema.avro.cdm19.UnitDependency bbnUnitDependency){
        Subject oldSubject=null,newSubject=null;
        oldSubject=subjectRepository.findById(UUID.nameUUIDFromBytes(bbnUnitDependency.getUnit().bytes())).orElse(null);
        newSubject=subjectRepository.findById(UUID.nameUUIDFromBytes(bbnUnitDependency.getDependentUnit().bytes())).orElse(null);
        UnitDependency unitDependency=new UnitDependency(oldSubject,newSubject);
        return unitDependencyRepository.save(unitDependency);
    }
}

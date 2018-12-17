package edu.nu.forensic.db.entity;


import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class FileObject extends Object{
    private Integer fileDescriptor;

    @ManyToOne
    private Principal localPrincipal;

    private Long size;

    public FileObject(UUID id, Integer fileDescriptor, Principal localPrincipal, Long size) {
        this.setId(id);
        this.fileDescriptor = fileDescriptor;
        this.localPrincipal = localPrincipal;
        this.size = size;
    }

    public FileObject() {
    }

    public Integer getFileDescriptor() {
        return fileDescriptor;
    }

    public void setFileDescriptor(Integer fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
    }

    public Principal getLocalPrincipal() {
        return localPrincipal;
    }

    public void setLocalPrincipal(Principal localPrincipal) {
        this.localPrincipal = localPrincipal;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}

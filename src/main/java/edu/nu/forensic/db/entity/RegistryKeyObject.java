package edu.nu.forensic.db.entity;


import java.util.UUID;

public class RegistryKeyObject extends Object{
    private Long size;

    private String key;

    public RegistryKeyObject(UUID id,Long size, String key) {
        this.size = size;
        this.key = key;
    }

    public RegistryKeyObject() {
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

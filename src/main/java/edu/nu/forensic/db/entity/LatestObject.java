package edu.nu.forensic.db.entity;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LatestObject {

    private long timestampNanos;
    private String name;
    private UUID id;

    public LatestObject() {
    }

    public LatestObject(UUID id, String name, long timestampNanos) {
        this.id = id;
        this.name = name;
        this.timestampNanos = timestampNanos;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public void setTimestampNanos(long timestampNanos) {
        this.timestampNanos = timestampNanos;
    }

    public boolean containsFilename(String filename){
        int k = 0, offset = 0;
        StringBuffer stringBuffer = new StringBuffer(filename);
        while((offset=stringBuffer.indexOf("\\", k)) >= 0){
            stringBuffer.insert(offset, "\\");
            k = offset+2;
        }
        Pattern pattern = Pattern.compile(stringBuffer.toString());
        Matcher matcher = pattern.matcher(this.name);
        if (matcher.find())
            return true;
        else
            return false;
    }
}

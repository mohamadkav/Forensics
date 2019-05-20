package edu.nu.forensic.JsonFormat;


public class ETWJsonFormatEvent {
    public EventType eventType;
    public String uuid;
    public String subject;
    public String object;

    public ETWJsonFormatEvent(EventType eventType, String uuid, String subject, String object) {
        this.eventType = eventType;
        this.uuid = uuid;
        this.subject = subject;
        this.object = object;
    }
}

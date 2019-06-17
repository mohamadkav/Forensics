package edu.nu.forensic.dto;

public class ReadTraceRequest {
    private String trace;

    public ReadTraceRequest(String trace) {
        this.trace = trace;
    }

    public ReadTraceRequest() {
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }
}

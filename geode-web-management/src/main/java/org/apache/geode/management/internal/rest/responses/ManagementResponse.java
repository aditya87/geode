package org.apache.geode.management.internal.rest.responses;

public class ManagementResponse {
    private Metadata metadata;
    private String message;

    public ManagementResponse() {}

    public ManagementResponse(Metadata metadata, String message) {
        this.metadata = metadata;
        this.message = message;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getMessage() {
        return message;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

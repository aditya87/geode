package org.apache.geode.management.internal.rest.responses;

public class Metadata {
    private String url;

    public Metadata() {
    }

    public Metadata(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

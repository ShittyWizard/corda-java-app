package com.template.web.dto;

public class NodeInfoResource {
    private String address;
    private int platformVersion;
    private long serial;
    private String partyName;

    public NodeInfoResource() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(int platformVersion) {
        this.platformVersion = platformVersion;
    }

    public long getSerial() {
        return serial;
    }

    public void setSerial(long serial) {
        this.serial = serial;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }
}

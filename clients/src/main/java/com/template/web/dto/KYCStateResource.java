package com.template.web.dto;

import java.util.Date;

public class KYCStateResource {
    private String partyNameSourceBank;
    private String partyNameTargetBank;
    private Date createTs;
    private String hashOfFile;
    private String sha256;

    public KYCStateResource() {
    }

    public String getPartyNameSourceBank() {
        return partyNameSourceBank;
    }

    public void setPartyNameSourceBank(String partyNameSourceBank) {
        this.partyNameSourceBank = partyNameSourceBank;
    }

    public String getPartyNameTargetBank() {
        return partyNameTargetBank;
    }

    public void setPartyNameTargetBank(String partyNameTargetBank) {
        this.partyNameTargetBank = partyNameTargetBank;
    }

    public Date getCreateTs() {
        return createTs;
    }

    public void setCreateTs(Date createTs) {
        this.createTs = createTs;
    }

    public String getHashOfFile() {
        return hashOfFile;
    }

    public void setHashOfFile(String hashOfFile) {
        this.hashOfFile = hashOfFile;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }
}

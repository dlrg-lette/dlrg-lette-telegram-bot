package org.dlrg.lette.telegrambot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties("webhook")
public class WebhookConfig {

    private String adminUUID;
    private String externalAdminAddress;
    private String externalAdminPort;
    private String externalAdminUrl;

    private String senderUUID;
    private String externalSenderAddress;
    private String externalSenderPort;
    private String externalSenderUrl;

    public WebhookConfig() {
        adminUUID = UUID.randomUUID().toString();
        senderUUID = UUID.randomUUID().toString();
    }

    public String getAdminUUID() {
        return adminUUID;
    }

    public void setAdminUUID(String adminUUID) {
        this.adminUUID = adminUUID;
    }

    public String getExternalAdminAddress() {
        return externalAdminAddress;
    }

    public void setExternalAdminAddress(String externalAdminAddress) {
        this.externalAdminAddress = externalAdminAddress;
    }

    public String getExternalAdminPort() {
        return externalAdminPort;
    }

    public void setExternalAdminPort(String externalAdminPort) {
        this.externalAdminPort = externalAdminPort;
    }

    public String getExternalAdminUrl() {
        return externalAdminUrl;
    }

    public void setExternalAdminUrl(String externalAdminUrl) {
        this.externalAdminUrl = externalAdminUrl;
    }

    public String getSenderUUID() {
        return senderUUID;
    }

    public void setSenderUUID(String senderUUID) {
        this.senderUUID = senderUUID;
    }

    public String getExternalSenderAddress() {
        return externalSenderAddress;
    }

    public void setExternalSenderAddress(String externalSenderAddress) {
        this.externalSenderAddress = externalSenderAddress;
    }

    public String getExternalSenderPort() {
        return externalSenderPort;
    }

    public void setExternalSenderPort(String externalSenderPort) {
        this.externalSenderPort = externalSenderPort;
    }

    public String getExternalSenderUrl() {
        return externalSenderUrl;
    }

    public void setExternalSenderUrl(String externalSenderUrl) {
        this.externalSenderUrl = externalSenderUrl;
    }
}

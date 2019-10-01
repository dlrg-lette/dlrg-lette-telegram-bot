package org.dlrg.lette.telegrambot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auth")
public class AuthConfig {

    private String adminBotToken;
    private String senderBotToken;

    public AuthConfig() {
    }

    public String getAdminBotToken() {
        return adminBotToken;
    }

    public void setAdminBotToken(String adminBotToken) {
        this.adminBotToken = adminBotToken;
    }

    public String getSenderBotToken() {
        return senderBotToken;
    }

    public void setSenderBotToken(String senderBotToken) {
        this.senderBotToken = senderBotToken;
    }
}

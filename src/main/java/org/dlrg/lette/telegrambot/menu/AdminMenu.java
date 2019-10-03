package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.model.Update;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.AuthConfig;
import org.dlrg.lette.telegrambot.WebhookConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminMenu {
    private static final Logger log = LogManager.getLogger(AdminMenu.class);

    private WebhookConfig webhookConfig;
    private AuthConfig authConfig;

    @Autowired
    public AdminMenu(WebhookConfig webhookConfig, AuthConfig authConfig) {
        this.webhookConfig = webhookConfig;
        this.authConfig = authConfig;
    }

    public void processUpdate(Update update) {

    }
}

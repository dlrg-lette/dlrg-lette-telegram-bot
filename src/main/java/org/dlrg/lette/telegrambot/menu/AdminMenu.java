package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.model.Update;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.AuthConfig;
import org.dlrg.lette.telegrambot.WebhookConfig;
import org.springframework.beans.factory.annotation.Autowired;

public class AdminMenu {
    private static final Logger log = LogManager.getLogger(AdminMenu.class);

    @Autowired
    private WebhookConfig webhookConfig;

    @Autowired
    private AuthConfig authConfig;

    public static void processUpdate(Update update) {

    }
}

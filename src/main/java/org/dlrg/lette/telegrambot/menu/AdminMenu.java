package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.model.Update;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.AuthConfig;
import org.dlrg.lette.telegrambot.WebhookConfig;
import org.springframework.beans.factory.annotation.Autowired;

public class AdminMenu {
    private static final Logger log = LogManager.getLogger(AdminMenu.class);

    private static AdminMenu instance;

    @Autowired
    private WebhookConfig webhookConfig;

    @Autowired
    private AuthConfig authConfig;

    private AdminMenu() { }

    public static AdminMenu getInstance() {
        if (instance == null) {
            instance = new AdminMenu();
        }
        return instance;
    }

    public void processUpdate(Update update) {

    }
}

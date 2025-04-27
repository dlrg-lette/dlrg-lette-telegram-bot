package org.dlrg.lette.telegrambot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.menu.AdminMenu;
import org.dlrg.lette.telegrambot.menu.SenderMenu;
import org.dlrg.lette.telegrambot.misc.Healtcheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Configuration
@EnableConfigurationProperties({WebhookConfig.class, AuthConfig.class})
@EnableAsync
public class TelegramBotApplication {

    private static final Logger log = LogManager.getLogger(TelegramBotApplication.class);

    private static AuthConfig authConfig;
    private static SenderMenu senderMenu;
    private static AdminMenu adminMenu;

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotApplication.class, args);

        // Webhooks registrieren
        // Admin
        TelegramBot adminBot = new TelegramBot(authConfig.getAdminBotToken());
        adminBot.setUpdatesListener(updates -> {
            updates.parallelStream().forEach(update -> {
                adminMenu.processUpdate(update, authConfig.getAdminBotToken(), authConfig.getSenderBotToken());
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                // got bad response from telegram
                log.error("{}: {}", e.response().errorCode(), e.response().description());
            } else {
                // probably network error
                log.error(e);
            }
        });
        Healtcheck.setAdminBotOK(true);

        // Sender
        TelegramBot senderBot = new TelegramBot(authConfig.getSenderBotToken());
        senderBot.setUpdatesListener(updates -> {
            updates.parallelStream().forEach(update -> {
                senderMenu.processUpdate(update, authConfig.getSenderBotToken());
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                // got bad response from telegram
                log.error("{}: {}", e.response().errorCode(), e.response().description());
            } else {
                // probably network error
                log.error(e);
            }
        });

        Healtcheck.setSenderBotOK(true);
    }

    @Autowired
    public void setAuthConfig(AuthConfig authConfig) {
        TelegramBotApplication.authConfig = authConfig;
    }

    @Autowired
    public void setAdminMenu(AdminMenu adminMenu) {
        TelegramBotApplication.adminMenu = adminMenu;
    }

    @Autowired
    public void setSenderMenu(SenderMenu senderMenu) {
        TelegramBotApplication.senderMenu = senderMenu;
    }
}

package org.dlrg.lette.telegrambot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.SetWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@SpringBootApplication
@Configuration
@EnableConfigurationProperties({WebhookConfig.class, AuthConfig.class})
public class TelegramBotApplication {

    private static final Logger log = LogManager.getLogger(TelegramBotApplication.class);

    private static WebhookConfig webhookConfig;
    private static AuthConfig authConfig;

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotApplication.class, args);

        // Webhooks registrieren
        // Admin
        TelegramBot adminBot = new TelegramBot(authConfig.getAdminBotToken());
        String adminUrl = webhookConfig.getExternalAdminUrl() + webhookConfig.getAdminUUID();
        log.debug("Admin webhook address: " + adminUrl);
        SetWebhook setAdminWebhook = new SetWebhook().url(adminUrl);
        BaseResponse adminResponse = adminBot.execute(setAdminWebhook);

        if (adminResponse.isOk()) {
            log.info("Admin Webhook successful registered.");
        } else {
            log.error(adminResponse.description());
        }

        // Sender
        TelegramBot senderBot = new TelegramBot(authConfig.getSenderBotToken());
        String senderUrl = webhookConfig.getExternalSenderUrl() + webhookConfig.getSenderUUID();
        log.debug("Sender webhook address: " + senderUrl);
        SetWebhook setSenderWebhook = new SetWebhook().url(senderUrl);
        BaseResponse senderResponse = senderBot.execute(setSenderWebhook);

        if (senderResponse.isOk()) {
            log.info("Sender webhook successful registered.");
        } else {
            log.error(senderResponse.description());
        }
    }

    @PreDestroy
    public static void unregisterWebhooks() {
        DeleteWebhook deleteWebhook = new DeleteWebhook();

        // Unregister WebHook for Admin-Bot
        log.info("Delete webhook for Admin Bot...");
        TelegramBot adminBot = new TelegramBot(authConfig.getAdminBotToken());
        BaseResponse response = adminBot.execute(deleteWebhook);
        log.info("Deletion successful for Admin-Bot: " + response.isOk());

        // Unregister WebHook for Sender-Bot
        TelegramBot senderBot = new TelegramBot(authConfig.getAdminBotToken());
        response = senderBot.execute(deleteWebhook);
        log.info("Deletion successful for Sender-Bot: " + response.isOk());

        log.info("Shutting down...");
    }

    @Autowired
    public void setWebhookConfig(WebhookConfig webhookConfig) {
        TelegramBotApplication.webhookConfig = webhookConfig;
    }

    @Autowired
    public void setAuthConfig(AuthConfig authConfig) {
        TelegramBotApplication.authConfig = authConfig;
    }

}

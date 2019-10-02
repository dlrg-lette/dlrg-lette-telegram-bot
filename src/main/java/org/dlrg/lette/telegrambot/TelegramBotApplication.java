package org.dlrg.lette.telegrambot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.SetWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
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

    private static WebhookConfig webhookConfig;
    private static AuthConfig authConfig;

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotApplication.class, args);

        // Webhooks registrieren
        // Admin
        TelegramBot adminBot = new TelegramBot(authConfig.getAdminBotToken());
        String adminUrl = webhookConfig.getExternalAdminUrl() + webhookConfig.getAdminUUID();
        SetWebhook setAdminWebhook = new SetWebhook().url(adminUrl);
        BaseResponse adminResponse = adminBot.execute(setAdminWebhook);

        if (adminResponse.isOk()) {
            System.out.println(adminUrl);
            System.out.println("Admin Webhook successful registered.");
        } else {
            System.err.println(adminResponse.description());
        }

        // Sender
        TelegramBot senderBot = new TelegramBot(authConfig.getSenderBotToken());
        String senderUrl = webhookConfig.getExternalSenderUrl() + webhookConfig.getSenderUUID();
        SetWebhook setSenderWebhook = new SetWebhook().url(senderUrl);
        BaseResponse senderResponse = senderBot.execute(setSenderWebhook);

        if (senderResponse.isOk()) {
            System.out.println(senderUrl);
            System.out.println("Sender Webhook successful registered.");
        } else {
            System.err.println(senderResponse.description());
        }
    }

    @PreDestroy
    public static void unregisterWebhooks() {
        DeleteWebhook deleteWebhook = new DeleteWebhook();

		// Unregister WebHook for Admin-Bot
		System.out.println("Delete webhook for Admin Bot...");
		TelegramBot adminBot = new TelegramBot(authConfig.getAdminBotToken());
		BaseResponse response = adminBot.execute(deleteWebhook);
		System.out.println("Deletion successful for Admin-Bot: " + response.isOk());

		// Unregister WebHook for Sender-Bot
		TelegramBot senderBot = new TelegramBot(authConfig.getAdminBotToken());
		response = senderBot.execute(deleteWebhook);
        System.out.println("Deletion successful for Sender-Bot: " + response.isOk());

        System.out.println("Shutting down...");
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

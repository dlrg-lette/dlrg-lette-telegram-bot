package org.dlrg.lette.telegrambot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.DeleteWebhook;
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

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotApplication.class, args);
    }

    @PreDestroy
    public static void unregisterWebhooks() {
		/*DeleteWebhook deleteWebhook = new DeleteWebhook();

		// Unregister WebHook for Admin-Bot
		System.out.println("Delete webhook for Admin Bot...");
		TelegramBot adminBot = new TelegramBot(authConfig.getAdminBotToken());
		BaseResponse response = adminBot.execute(deleteWebhook);
		System.out.println("Deletion successful for Admin-Bot: " + response.isOk());

		// Unregister WebHook for Sender-Bot
		TelegramBot senderBot = new TelegramBot(authConfig.getAdminBotToken());
		response = senderBot.execute(deleteWebhook);
		System.out.println("Deletion successful for Sender-Bot: " + response.isOk());*/

        System.out.println("Shutting down...");
    }

}

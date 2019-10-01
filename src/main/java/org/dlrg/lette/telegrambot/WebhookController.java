package org.dlrg.lette.telegrambot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.SetWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebhookController {

    @Autowired
    private WebhookConfig webhookConfig;

    @Autowired
    private AuthConfig authConfig;

    // Register Webhooks
    @RequestMapping("/register")
    public void registerWebhooks() {
        // Webhooks registrieren
        // Admin
        TelegramBot adminBot = new TelegramBot(authConfig.getAdminBotToken());
        SetWebhook setAdminWebhook = new SetWebhook().url(webhookConfig.getExternalAdminUrl());
        BaseResponse adminResponse = adminBot.execute(setAdminWebhook);

        if (adminResponse.isOk()) {
            System.out.println(webhookConfig.getExternalAdminUrl());
            System.out.println("Admin Webhook successful registered.");
        } else {
            System.err.println(adminResponse.description());
        }

        // Sender
        TelegramBot senderBot = new TelegramBot(authConfig.getSenderBotToken());
        SetWebhook setSenderWebhook = new SetWebhook().url(webhookConfig.getExternalSenderUrl());
        BaseResponse senderResponse = senderBot.execute(setSenderWebhook);

        if (senderResponse.isOk()) {
            System.out.println(webhookConfig.getExternalSenderUrl());
            System.out.println("Sender Webhook successful registered.");
        } else {
            System.err.println(senderResponse.description());
        }
    }

    // Update
    @RequestMapping(method = RequestMethod.POST, path = "/update/{uuid}")
    public ResponseEntity receiveUpdate(@PathVariable("uuid") String uuid, @RequestBody Update update) {
        System.out.println("Update received, UUID: " + uuid);
        System.out.println("Admin UUID: " + webhookConfig.getAdminUUID());
        System.out.println("Sender UUID: " + webhookConfig.getSenderUUID());
        if (uuid.equalsIgnoreCase(webhookConfig.getAdminUUID())) {
            // Admin Bot
            System.out.println("Admin Update Received!");
            System.out.println(update.message().text());
        } else if (uuid.equalsIgnoreCase(webhookConfig.getSenderUUID())) {
            // Sender Bot
            System.out.println("Sender Update Received!");
            System.out.println(update.message().text());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // Delete Webhooks
    @RequestMapping(method = RequestMethod.GET, path = "/deregister")
    public void delteWebhooks() {
        DeleteWebhook deleteWebhook = new DeleteWebhook();

		// Unregister WebHook for Admin-Bot
		System.out.println("Delete webhook for Admin Bot...");
		TelegramBot adminBot = new TelegramBot(authConfig.getAdminBotToken());
		BaseResponse response = adminBot.execute(deleteWebhook);
		System.out.println("Deletion successful for Admin-Bot: " + response.isOk());

		// Unregister WebHook for Sender-Bot
        System.out.println("Delete webhook for Sender Bot...");
		TelegramBot senderBot = new TelegramBot(authConfig.getAdminBotToken());
		response = senderBot.execute(deleteWebhook);
		System.out.println("Deletion successful for Sender-Bot: " + response.isOk());
    }

}

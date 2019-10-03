package org.dlrg.lette.telegrambot;

import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.model.Update;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.data.Chat;
import org.dlrg.lette.telegrambot.data.ChatRepository;
import org.dlrg.lette.telegrambot.menu.AdminMenu;
import org.dlrg.lette.telegrambot.menu.SenderMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebhookController {
    private static final Logger log = LogManager.getLogger(WebhookController.class);

    @Autowired
    private WebhookConfig webhookConfig;

    @Autowired
    private AuthConfig authConfig;

    @Autowired
    private ChatRepository chatRepository;

    // Update
    @RequestMapping(method = RequestMethod.POST, path = "/update/{uuid}")
    public ResponseEntity receiveUpdate(@PathVariable("uuid") String uuid, @RequestBody String updateString) {

        // Parse Update String to Object
        Update update = BotUtils.parseUpdate(updateString);

        log.debug("Update received, UUID: " + uuid);
        if (uuid.equalsIgnoreCase(webhookConfig.getAdminUUID())) {
            // Admin Bot
            log.debug("Admin Update Received, processing in asynchronous task...");
            new Thread( () -> AdminMenu.getInstance().processUpdate(update)).start();
        } else if (uuid.equalsIgnoreCase(webhookConfig.getSenderUUID())) {
            // Sender Bot
            log.debug("Sender Update Received, processing in asynchronous task...");
            new Thread( () -> SenderMenu.getInstance().processUpdate(update, authConfig.getSenderBotToken())).start();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

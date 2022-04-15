package org.dlrg.lette.telegrambot.misc;

import org.dlrg.lette.telegrambot.data.ModeratorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class Healtcheck {

    private static boolean adminBotOK = false;
    private static boolean senderBotOK = false;
    private final ModeratorRepository moderatorRepository;

    @Autowired
    public Healtcheck(ModeratorRepository moderatorRepository) {
        this.moderatorRepository = moderatorRepository;
    }

    public static void setAdminBotOK(boolean adminBotOK) {
        Healtcheck.adminBotOK = adminBotOK;
    }

    public static void setSenderBotOK(boolean senderBotOK) {
        Healtcheck.senderBotOK = senderBotOK;
    }

    public ResponseEntity getServiceHealth() {
        // Check mongoDB connection by listing all moderators
        boolean mongoConnectionOK;
        try {
            moderatorRepository.findAll();
            mongoConnectionOK = true;
        } catch (Exception ignored) {
            mongoConnectionOK = false;
        }

        String body = String.format("adminBotOK: %s\r\nsenderBotOK: %s\r\nmongoConnection: %s", adminBotOK, senderBotOK, mongoConnectionOK);

        if (adminBotOK && senderBotOK && mongoConnectionOK) {
            return ResponseEntity.status(HttpStatus.OK).body(body);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}

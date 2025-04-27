package org.dlrg.lette.telegrambot;

import org.apache.logging.log4j.LogManager;
import org.dlrg.lette.telegrambot.misc.Healtcheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {
    static {
        LogManager.getLogger(WebhookController.class);
    }

    private final Healtcheck healthCheck;

    @Autowired
    public WebhookController(Healtcheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    // Prevent access to other resources, like MongoDB direct Data access
    @GetMapping()
    public ResponseEntity denyAllOtherAccess() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Healthcheck
    @GetMapping(path = "/healthcheck")
    public ResponseEntity returnHealthcheck() {
        return healthCheck.getServiceHealth();
    }
}

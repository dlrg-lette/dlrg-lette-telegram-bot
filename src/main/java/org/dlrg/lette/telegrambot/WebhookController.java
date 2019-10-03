package org.dlrg.lette.telegrambot;

import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.model.Update;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.data.Chat;
import org.dlrg.lette.telegrambot.data.ChatRepository;
import org.dlrg.lette.telegrambot.data.Customer;
import org.dlrg.lette.telegrambot.data.CustomerRepository;
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
    private CustomerRepository repository;

    @Autowired
    private static ChatRepository chatRepository;

    // Update
    @RequestMapping(method = RequestMethod.POST, path = "/update/{uuid}")
    public ResponseEntity receiveUpdate(@PathVariable("uuid") String uuid, @RequestBody String updateString) {

        /*repository.deleteAll();

        // save a couple of customers
        repository.save(new Customer("Alice", "Smith"));
        repository.save(new Customer("Bob", "Smith"));

        // fetch all customers
        System.out.println("Customers found with findAll():");
        System.out.println("-------------------------------");
        for (Customer customer : repository.findAll()) {
            System.out.println(customer);
        }
        System.out.println();

        // fetch an individual customer
        System.out.println("Customer found with findByFirstName('Alice'):");
        System.out.println("--------------------------------");
        System.out.println(repository.findByFirstName("Alice"));

        System.out.println("Customers found with findByLastName('Smith'):");
        System.out.println("--------------------------------");
        for (Customer customer : repository.findByLastName("Smith")) {
            System.out.println(customer);
        }*/

        chatRepository.deleteAll();

        /*Update update = BotUtils.parseUpdate(updateString);
        log.info("Update");
        Chat newChat = new Chat("status", "message");
        chatRepository.save(newChat);*/

        /*// Parse Update String to Object
        Update update = BotUtils.parseUpdate(updateString);

        log.debug("Update received, UUID: " + uuid);
        if (uuid.equalsIgnoreCase(webhookConfig.getAdminUUID())) {
            // Admin Bot
            log.debug("Admin Update Received, processing in asynchronous task...");
            new Thread( () -> AdminMenu.processUpdate(update)).start();
        } else if (uuid.equalsIgnoreCase(webhookConfig.getSenderUUID())) {
            // Sender Bot
            log.debug("Sender Update Received, processing in asynchronous task...");
            new Thread( () -> SenderMenu.processUpdate(update, authConfig.getSenderBotToken())).start();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }*/
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

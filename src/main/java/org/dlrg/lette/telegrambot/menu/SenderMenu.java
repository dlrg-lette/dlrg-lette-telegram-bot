package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.data.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

public class SenderMenu {
    private static final Logger log = LogManager.getLogger(SenderMenu.class);

    private static SenderMenu instance;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private SenderMenu() { }

    public static SenderMenu getInstance() {
        if (instance == null) {
            instance = new SenderMenu();
        }
        return instance;
    }

    public void processUpdate(Update update, String senderBotToken) {
        TelegramBot adminBot = new TelegramBot(senderBotToken);


        // Prüfen ob normale Nachricht / Kommando oder Inline-Query Result
        if (update.message() != null) {
            // Prüe Kommando
            switch (update.message().text()) {
                case "/start":
                case "/abonnieren":
                    chatRepository.save(new Chat(update.message().chat().id(), "abonnieren", ""));

                    // Kategorien abrufen
                    List<Category> categories = categoryRepository.findAll();

                    // Abonnierte Kategorien des Users ermitteln
                    int currentUserId = update.message().contact().userId();
                    Optional<User> optionalUser = userRepository.findById(currentUserId);
                    if (optionalUser.isPresent()) {
                        List<Category> userCategories = optionalUser.get().categories;
                    } else {
                        // Benutzer anlegen
                        userRepository.save(new User(update.message().contact().userId()));
                    }

                    // Buttons zurückgeben

                    break;
            }
            if (update.message().text().equals("/inline")) {
                SendMessage sendMessage = new SendMessage(update.message().chat().id(), "Test inline Buttons...");
                sendMessage.replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton[]{new InlineKeyboardButton("Button 1").callbackData("1"), new InlineKeyboardButton("Button 2").callbackData("2")}));

                BaseResponse inlineMessage = adminBot.execute(sendMessage);

                if (!inlineMessage.isOk()) {
                    log.error(String.format("%d - %s", inlineMessage.errorCode(), inlineMessage.description()));
                } else {
                    log.info("Mongo Entry exists: " + chatRepository.existsById(update.message().chat().id()));

                    log.info("Chat ID: " + update.message().chat().id());
                    Chat newChat = new Chat(update.message().chat().id(), "status", "message");
                    chatRepository.save(newChat);
                }
            }
        }

        if (update.callbackQuery() != null) {
            SendMessage sendMessage = new SendMessage(update.callbackQuery().message().chat().id(), "Button " + update.callbackQuery().data() + " wurde gedrückt!");
            BaseResponse inlineMessage = adminBot.execute(sendMessage);
            if (!inlineMessage.isOk()) {
                log.error(String.format("%d - %s", inlineMessage.errorCode(), inlineMessage.description()));
            }
        }
    }
}

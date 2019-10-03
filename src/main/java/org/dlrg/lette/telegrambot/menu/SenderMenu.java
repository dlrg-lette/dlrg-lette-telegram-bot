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

import java.util.Collections;
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

    private SenderMenu() {
    }

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
            long chatId = update.message().chat().id();
            int userId = update.message().contact().userId();

            // Prüe Kommando
            switch (update.message().text()) {
                case "/start":
                case "/abonnieren":
                    chatRepository.save(new Chat(chatId, "abonnieren"));
                    sendCategoryStatus(adminBot, userId, chatId);
                    break;

                case "/ende":
                case "/deabonnieren":
                    chatRepository.save(new Chat(chatId, "deabonnieren"));
                    sendCategoryStatus(adminBot, userId, chatId);
                    break;

                case "/abbrechen":
                    // Status entfernen ubnd Antwort senden
                    chatRepository.deleteById(chatId);
                    break;
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

    private void sendCategoryStatus(TelegramBot bot, int userId, Long chatId) {


        /*
        Kategorien abrufen
        User Kategorien abrufen
        Button mit Text erstellen
        -> Symbol für Abo-Status hinzufügen
        Ausgeben
         */

        // Kategorien abrufen
        List<Category> categories = categoryRepository.findAll();

        // Abonnierte Kategorien des Users ermitteln
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            List<Integer> userCategories = optionalUser.get().categories;

        } else {
            // Benutzer anlegen
            userRepository.save(new User(userId));
        }

        // Buttons zurückgeben
        SendMessage sendMessage = new SendMessage(chatId, "Zu folgenden Kategorien können Informationen erhalten werden, bitte auswählen:");
        sendMessage.replyMarkup(
                new InlineKeyboardMarkup(
                        new InlineKeyboardButton[]{
                                new InlineKeyboardButton("Button 1").callbackData("1"),
                                new InlineKeyboardButton("Button 2").callbackData("2")
                        }));

        BaseResponse inlineMessage = bot.execute(sendMessage);

        if (!inlineMessage.isOk()) {
            log.error(inlineMessage.errorCode() + " - " + inlineMessage.description());
        }
    }
}

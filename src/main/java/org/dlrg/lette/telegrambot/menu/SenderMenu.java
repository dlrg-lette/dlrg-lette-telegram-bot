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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SenderMenu {
    private static final Logger log = LogManager.getLogger(SenderMenu.class);

    private ChatRepository chatRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;

    @Autowired
    public SenderMenu(ChatRepository chatRepository, CategoryRepository categoryRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public void processUpdate(Update update, String senderBotToken) {
        TelegramBot adminBot = new TelegramBot(senderBotToken);


        // Prüfen ob normale Nachricht / Kommando oder Inline-Query Result
        if (update.message() != null) {
            long chatId = update.message().chat().id();
            int userId = update.message().from().id();

            // Prüe Kommando
            switch (update.message().text()) {
                case "/start":
                case "/abos":
                    if (chatRepository.existsById(chatId)) {
                        chatRepository.save(new Chat(chatId, "abos"));
                    } else {
                        chatRepository.insert(new Chat(chatId, "abos"));
                    }
                    sendCategoryStatus(adminBot, userId, chatId);
                    break;

                case "/ende":
                    if (chatRepository.existsById(chatId)) {
                        chatRepository.save(new Chat(chatId, "deabonnieren"));
                    } else {
                        chatRepository.insert(new Chat(chatId, "deabonnieren"));
                    }
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
        -> Haken: \u2705 - ✅
        -> :no_entry: \26d4 - ⛔
        Ausgeben
         */

        // Kategorien abrufen
        List<Category> categories = categoryRepository.findAll();

        // Abonnierte Kategorien des Users ermitteln
        Optional<User> optionalUser = userRepository.findById(userId);
        List<Integer> userCategories = new ArrayList<Integer>();
        if (optionalUser.isPresent()) {
            userCategories = optionalUser.get().categories;
        } else {
            // Benutzer anlegen
            userRepository.insert(new User(userId));
        }

        // Button mit Text erstellen
        List<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<InlineKeyboardButton>();

        // "Alle" Funktionsbutton
//        new InlineKeyboardButton("Alle Kategorien").callbackData("all")

        for (Category category : categories) {
            String btnText = "";
            String callbackData = "";

            // Ist die Kategorie bereits abonniert?
            if (userCategories.contains(category.id)) {
                // Ist bereits abonniert
                btnText = String.format("%s ✅", category.description);
                callbackData = category.id + ";true";
            } else {
                // Ist nicht abonniert
                btnText = String.format("%s ⛔", category.description);
                callbackData = category.id + ";false";
            }
            // Button erstellen und zur Liste hinzufügen
            inlineKeyboardButtons.add(new InlineKeyboardButton(btnText).callbackData(callbackData));
        }

        // Buttons zurückgeben
        SendMessage sendMessage = new SendMessage(chatId, "Zu folgenden Kategorien können Informationen erhalten werden, bitte auswählen:");

        // Liste zu Array konvertieren
        InlineKeyboardButton[] keyboardButtons = new InlineKeyboardButton[inlineKeyboardButtons.size()];
        keyboardButtons = inlineKeyboardButtons.toArray(keyboardButtons);

        // Button zum Call hinzufügen
        sendMessage.replyMarkup(new InlineKeyboardMarkup(keyboardButtons));

        // Senden
        BaseResponse inlineMessage = bot.execute(sendMessage);

        if (!inlineMessage.isOk()) {
            log.error(inlineMessage.errorCode() + " - " + inlineMessage.description());
        }
    }
}

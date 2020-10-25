package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.LeaveChat;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SenderMenu {
    private static final Logger log = LogManager.getLogger(SenderMenu.class);

    private final SenderChatRepository senderChatRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TextRepository texts;

    @Autowired
    public SenderMenu(SenderChatRepository senderChatRepository, CategoryRepository categoryRepository, UserRepository userRepository, TextRepository textRepository) {
        this.senderChatRepository = senderChatRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.texts = textRepository;
    }

    @Async
    public void processUpdate(Update update, String senderBotToken) {
        try {
            TelegramBot senderBot = new TelegramBot(senderBotToken);

            // Normale Nachricht / Kommando
            if (update.message() != null) {
                long chatId = update.message().chat().id();
                int userId = update.message().from().id();

                // Prüe Kommando
                switch (update.message().text()) {
                    case "/start":
                        sendWelcomeMessage(senderBot, chatId, userId, update.message().from().firstName());
                        senderChatRepository.save(new SenderChat(chatId, "abos"));
                        sendCategoryStatus(senderBot, userId, chatId, null);
                        break;

                    case "/abos":
                        senderChatRepository.save(new SenderChat(chatId, "abos"));
                        sendCategoryStatus(senderBot, userId, chatId, null);
                        break;

                    case "/ende":
                        // User löschen
                        userRepository.deleteById(userId);
                        // ggf. Chat löschen
                        senderChatRepository.deleteById(chatId);

                        String endMessageText = texts.findById("END").get().text;
                        SendMessage endMessage = new SendMessage(chatId, String.format(endMessageText, update.message().from().firstName()));
                        senderBot.execute(endMessage);
                        LeaveChat leaveChat = new LeaveChat(chatId);
                        senderBot.execute(leaveChat);
                        break;

                    case "/abbrechen":
                        // Status entfernen und Antwort senden
                        senderChatRepository.deleteById(chatId);
                        String cancelMessageText = texts.findById("CANCEL").get().text;
                        SendMessage cancelMessage = new SendMessage(chatId, cancelMessageText);
                        senderBot.execute(cancelMessage);
                        break;
                }
            }

            // Ergebnis der Buttons
            if (update.callbackQuery() != null) {
                long chatId = update.callbackQuery().message().chat().id();
                int userId = update.callbackQuery().from().id();
                String data = update.callbackQuery().data();
                int messageId = update.callbackQuery().message().messageId();

                // Prüfe ob Menü beendet wurde
                if (data.equals("cancel")) {
                    EditMessageText showFinishedText = new EditMessageText(chatId, messageId, texts.findById("FINISHED_ABOS").get().text);
                    BaseResponse response = senderBot.execute(showFinishedText);

                    if (!response.isOk()) {
                        log.error(String.format("Error while sending Abos finished message: %d - %s", response.errorCode(), response.description()));
                    }
                    return;
                }

                // Chat Status abrufen
                Optional<SenderChat> optionalChat = senderChatRepository.findById(chatId);
                String chatStatus = "";
                if (optionalChat.isPresent()) {
                    chatStatus = optionalChat.get().status;
                }

                // Weiteres Vorgehen je nach Chat Status
                if ("abos".equals(chatStatus)) {
                    // Abo-Antwort verarbeiten
                    processAboResponse(userId, data);
                    // Aktualisierten Status ausgeben
                    sendCategoryStatus(senderBot, userId, chatId, messageId);
                } else {
                    log.error(String.format("BEAR - Chat mit unbekanntem Status: %s", chatStatus));
                    String errorMessage = texts.findById("BEAR").get().text;
                    SendMessage sendMessage = new SendMessage(chatId, String.format(errorMessage, chatId, LocalDateTime.now()));
                    senderBot.execute(sendMessage);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void sendWelcomeMessage(TelegramBot bot, Long chatId, Integer userId, String forename) {
        // User speichern (inkl. Name), nur wenn dieser nicht schon vorhanden ist
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            userRepository.save(new User(userId, forename));
        }

        String welcomeMessageText = texts.findById("WELCOME_SENDER").get().text;
        SendMessage welcomeMessage = new SendMessage(chatId, String.format(welcomeMessageText, forename));
        BaseResponse welcomeResponse = bot.execute(welcomeMessage);

        if (!welcomeResponse.isOk()) {
            log.error(String.format("Error while sending welcome_message: %d - %s", welcomeResponse.errorCode(), welcomeResponse.description()));
        }
    }

    private void processAboResponse(int userId, String data) {
        String[] result = data.split(";");
        int categoryId = Integer.parseInt(result[0]);
        boolean isSubscribed = Boolean.parseBoolean(result[1]);

        Optional<User> optionalUser = userRepository.findById(userId);
        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            // Benutzer anlegen
            user = new User(userId);
            user = userRepository.save(user);
        }

        if (isSubscribed) {
            // Abo beenden
            user.categories.removeIf(id -> id.equals(categoryId));
        } else {
            // Abo starten
            user.categories.add(categoryId);
        }

        // Aktualisierten User speichern
        userRepository.save(user);
    }

    private void sendCategoryStatus(TelegramBot bot, int userId, Long chatId, Integer messageId) {

        // Kategorien abrufen
        List<Category> categories = categoryRepository.findAll();

        // Abonnierte Kategorien des Users ermitteln
        Optional<User> optionalUser = userRepository.findById(userId);
        List<Integer> userCategories = new ArrayList<>();
        if (optionalUser.isPresent()) {
            userCategories = optionalUser.get().categories;
            if (userCategories == null) {
                userCategories = new ArrayList<>();
            }
        } else {
            // Benutzer anlegen
            userRepository.save(new User(userId));
        }

        // Buttons mit Text erstellen, jeweils neue Zeile + Zurück Button
        InlineKeyboardButton[][] buttons = new InlineKeyboardButton[categories.size() + 1][];

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            String btnText;
            String callbackData;

            // Ist die Kategorie bereits abonniert?
            if (userCategories.contains(category.id)) {
                // Ist bereits abonniert
                String subText = texts.findById("SUBSCRIBED").get().text;
                btnText = String.format(subText, category.description);
                callbackData = category.id + ";true";
            } else {
                // Ist nicht abonniert
                String unsubText = texts.findById("UNSUBSCRIBED").get().text;
                btnText = String.format(unsubText, category.description);
                callbackData = category.id + ";false";
            }

            buttons[i] = new InlineKeyboardButton[]{new InlineKeyboardButton(btnText).callbackData(callbackData)};
        }

        // Zurück Button hinzufügen
        buttons[buttons.length - 1] = new InlineKeyboardButton[]{new InlineKeyboardButton(texts.findById("BACK_ADMINISTRATION_BUTTON").get().text).callbackData("cancel")};

        // Inline Markup erstellen
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);

        // Falls Message-ID vorhanden bestehende Nachricht aktualisieren
        if (messageId == null) {
            // Neue Nachricht senden
            // Buttons zurückgeben
            String categorySelectionText = texts.findById("CATEGORY_SELECTION").get().text;
            SendMessage sendMessage = new SendMessage(chatId, categorySelectionText);

            // Button zum Call hinzufügen
            sendMessage.replyMarkup(markup);

            // Senden
            BaseResponse messageResponse = bot.execute(sendMessage);

            if (!messageResponse.isOk()) {
                log.error(messageResponse.errorCode() + " - " + messageResponse.description());
            }
        } else {
            // Bestehende Nachricht aktualisieren
            EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup(chatId, messageId).replyMarkup(markup);

            // Senden
            BaseResponse messageResponse = bot.execute(editMessageReplyMarkup);

            if (!messageResponse.isOk()) {
                log.error(messageResponse.errorCode() + " - " + messageResponse.description());
            }
        }
    }
}

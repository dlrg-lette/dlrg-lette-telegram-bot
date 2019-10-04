package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.LeaveChat;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SenderMenu {
    private static final Logger log = LogManager.getLogger(SenderMenu.class);

    private ChatRepository chatRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;
    private TextRepository texts;

    @Autowired
    public SenderMenu(ChatRepository chatRepository, CategoryRepository categoryRepository, UserRepository userRepository, TextRepository textRepository) {
        this.chatRepository = chatRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.texts = textRepository;
    }

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
                        sendWelcomeMessage(senderBot, chatId, update.message().from().firstName());
                        if (chatRepository.existsById(chatId)) {
                            chatRepository.save(new Chat(chatId, "abos"));
                        } else {
                            chatRepository.insert(new Chat(chatId, "abos"));
                        }
                        sendCategoryStatus(senderBot, userId, chatId);
                        break;

                    case "/abos":
                        if (chatRepository.existsById(chatId)) {
                            chatRepository.save(new Chat(chatId, "abos"));
                        } else {
                            chatRepository.insert(new Chat(chatId, "abos"));
                        }
                        sendCategoryStatus(senderBot, userId, chatId);
                        break;

                    case "/ende":
                        // User löschen
                        userRepository.deleteById(userId);

                        String endMessageText = texts.findById("END").get().text;
                        SendMessage endMessage = new SendMessage(chatId, endMessageText);
                        senderBot.execute(endMessage);
                        LeaveChat leaveChat = new LeaveChat(chatId);
                        senderBot.execute(leaveChat);
                        break;

                    case "/abbrechen":
                        // Status entfernen ubnd Antwort senden
                        chatRepository.deleteById(chatId);
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

                // Chat Status abrufen
                Optional<Chat> optionalChat = chatRepository.findById(chatId);
                String chatStatus = "";
                if (optionalChat.isPresent()) {
                    chatStatus = optionalChat.get().status;
                }

                // Weiteres Vorgehen je nach Chat Status
                if ("abos".equals(chatStatus)) {// Abo-Antwort verarbeiten
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
            log.error(e.getStackTrace());
        }
    }

    private void sendWelcomeMessage(TelegramBot bot, Long chatId, String forename) {

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
            user = userRepository.insert(user);
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

    private void sendCategoryStatus(TelegramBot bot, int userId, Long chatId) {
        sendCategoryStatus(bot, userId, chatId, null);
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
            userRepository.insert(new User(userId));
        }

        // Button mit Text erstellen
        List<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();

        for (Category category : categories) {
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
            // Button erstellen und zur Liste hinzufügen
            inlineKeyboardButtons.add(new InlineKeyboardButton(btnText).callbackData(callbackData));
        }

        // Button Liste zu Array konvertieren
        InlineKeyboardButton[] keyboardButtons = new InlineKeyboardButton[inlineKeyboardButtons.size()];
        keyboardButtons = inlineKeyboardButtons.toArray(keyboardButtons);

        // Falls Message-ID vorhanden bestehende Nachricht aktualisieren
        if (messageId == null) {
            // Neue Nachricht senden
            // Buttons zurückgeben
            String categorySelectionText = texts.findById("CATEGORY_SELECTION").get().text;
            SendMessage sendMessage = new SendMessage(chatId, categorySelectionText);

            // Button zum Call hinzufügen
            sendMessage.replyMarkup(new InlineKeyboardMarkup(keyboardButtons));

            // Senden
            BaseResponse messageResponse = bot.execute(sendMessage);

            if (!messageResponse.isOk()) {
                log.error(messageResponse.errorCode() + " - " + messageResponse.description());
            }
        } else {
            // Bestehende Nachricht aktualisieren
            EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup(chatId, messageId).replyMarkup(new InlineKeyboardMarkup(keyboardButtons));

            // Senden
            BaseResponse messageResponse = bot.execute(editMessageReplyMarkup);

            if (!messageResponse.isOk()) {
                log.error(messageResponse.errorCode() + " - " + messageResponse.description());
            }
        }
    }
}

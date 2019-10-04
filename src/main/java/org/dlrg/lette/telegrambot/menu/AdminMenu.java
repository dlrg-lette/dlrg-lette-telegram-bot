package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
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
public class AdminMenu {
    private static final Logger log = LogManager.getLogger(AdminMenu.class);

    private AdminChatRepository adminChatRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;
    private TextRepository texts;

    @Autowired
    public AdminMenu(AdminChatRepository adminChatRepository, CategoryRepository categoryRepository, UserRepository userRepository, TextRepository texts) {
        this.adminChatRepository = adminChatRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.texts = texts;
    }


    public void processUpdate(Update update, String adminBotToken, String senderBotToken) {
        try {
            // Bots erstellen
            TelegramBot adminBot = new TelegramBot(adminBotToken);
            TelegramBot senderBot = new TelegramBot(senderBotToken);

            // Normale Nachricht / Kommando
            if (update.message() != null) {
                long chatId = update.message().chat().id();

                // Chat Statusabfrage
                Optional<AdminChat> optionalCurrentChat = adminChatRepository.findById(chatId);

                // Unterscheidung nach Chat Status
                if (optionalCurrentChat.isPresent()) {
                    // Chat besitzt Status, prüfen und verarbeiten
                    AdminChat currentChat = optionalCurrentChat.get();

                    switch (currentChat.status) {
                        case "confirm":
                            confirmSendMessage(adminBot, chatId, update.message().text());
                            break;

                        default:
                            log.warn(String.format("Unknown chat status: %s", currentChat.status));
                            break;
                    }
                } else {
                    // Kein gespeicherter Chat Status, neu Beginnen:
                    switch (update.message().text()) {
                        case "/start":
                            sendWelcomeMessage(adminBot, chatId, update.message().from().firstName());
                            break;

                        case "/kategorien":
                            // todo Kategorien verwalten
                            break;

                        case "/nachricht":
                            // Chat Status setzen
                            AdminChat adminChat = new AdminChat(chatId, "message");
                            adminChatRepository.save(adminChat);

                            // Nachricht ausgeben mit Kategorien
                            sendMessageCategories(adminBot, chatId);
                            break;

                        case "/abbrechen":
                            // Status entfernen ubnd Antwort senden
                            adminChatRepository.deleteById(chatId);
                            String cancelMessageText = texts.findById("CANCEL").get().text;
                            SendMessage cancelMessage = new SendMessage(chatId, cancelMessageText);
                            adminBot.execute(cancelMessage);
                            break;

                        default:
                            log.warn(String.format("Unknown command: %s", update.message().text()));
                            String unknownCommandText = texts.findById("UNKNOWN_COMMAND").get().text;
                            SendMessage unknownCommandMessage = new SendMessage(chatId, unknownCommandText);
                            BaseResponse unknownCommandResponse = adminBot.execute(unknownCommandMessage);

                            if (!unknownCommandResponse.isOk()) {
                                log.error("Error while sending unknown command response to user: " + unknownCommandResponse.errorCode() + " - " + unknownCommandResponse.description());
                            }
                            break;
                    }
                }
            }

            // Ergebnis der Buttons
            if (update.callbackQuery() != null) {
                long chatId = update.callbackQuery().message().chat().id();
                String data = update.callbackQuery().data();
                int messageId = update.callbackQuery().message().messageId();

                // Chat Status abrufen
                Optional<AdminChat> optionalChat = adminChatRepository.findById(chatId);
                AdminChat currentChat = new AdminChat();
                if (optionalChat.isPresent()) {
                    currentChat = optionalChat.get();
                }

                switch (currentChat.status) {
                    case "message":
                        // Kategorie in AdminChat speichern, neuer Status
                        currentChat.category = Integer.parseInt(data);
                        currentChat.status = "confirm";
                        adminChatRepository.save(currentChat);

                        // Kategorie bestätigen, Aufforderung Nachrichteneingabe
                        confirmMessageCategory(adminBot, chatId, messageId, data);
                        break;

                    case "confirm":
                        // Chat Status löschen, egal was passiert
                        adminChatRepository.deleteById(chatId);

                        // Unterscheiden ob bestätigt oder abgebrochen wurde
                        if (Boolean.parseBoolean(data)) {
                            // Senden an alle Abonnenten
                            sendMessageToCategory(senderBot, currentChat.category, update.callbackQuery().message().text());

                            // Bestätigung an Moderator senden
                            String confirmMessage = texts.findById("CONFIRM_SEND_TO_MODERATOR").get().text;
                            int receiverAmount = userRepository.findAllByCategory(currentChat.category).size();
                            SendMessage confirmDelivery = new SendMessage(chatId, String.format(confirmMessage, receiverAmount));
                            BaseResponse confirmResponse = adminBot.execute(confirmDelivery);

                            if (!confirmResponse.isOk()) {
                                log.error(String.format("Error while sending confirm to moderator: %d - %s", confirmResponse.errorCode(), confirmResponse.description()));
                            }
                        } else {
                            // Update der finalen Nachricht bzw. des Markups -> Entfernen
                            cancelSendMessage(adminBot, chatId, messageId);
                        }
                        break;

                    default:
                        log.warn(String.format("Unknown query result %s to status %s", data, currentChat.status));
                        break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void sendWelcomeMessage(TelegramBot bot, Long chatId, String forename) {
        String welcomeMessageText = texts.findById("WELCOME_ADMIN").get().text;
        SendMessage welcomeMessage = new SendMessage(chatId, String.format(welcomeMessageText, forename));
        BaseResponse welcomeResponse = bot.execute(welcomeMessage);

        if (!welcomeResponse.isOk()) {
            log.error(String.format("Error while sending welcome_message: %d - %s", welcomeResponse.errorCode(), welcomeResponse.description()));
        }
    }

    private void cancelSendMessage(TelegramBot adminBot, long chatId, int messageId) {
        // ReplyMarkup der Original-Nachricht ändern
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup(chatId, messageId).replyMarkup(new InlineKeyboardMarkup());
        BaseResponse editMarkupResponse = adminBot.execute(editMessageReplyMarkup);

        if (!editMarkupResponse.isOk()) {
            log.error("Error while changing 'confirm_message' reply_markup: " + editMarkupResponse.errorCode() + " - " + editMarkupResponse.description());
        }

        // Bestätigung an User senden, dass abgebrochen wurde
        String confirmCancelText = texts.findById("CONFIRM_CANCEL").get().text;
        SendMessage confirmCancel = new SendMessage(chatId, confirmCancelText);
        BaseResponse confirmCancelResponse = adminBot.execute(confirmCancel);

        if (!confirmCancelResponse.isOk()) {
            log.error("Error while sending confirm_cancel " + confirmCancelResponse.errorCode() + " - " + confirmCancelResponse.description());
        }

    }

    private void sendMessageToCategory(TelegramBot senderBot, int category, String sendingMessageText) {
        // Alle Benutzer mit Kategorie ermitteln
        List<User> subscriber = userRepository.findAllByCategory(category);

        // Benutze User-ID als Chat-ID (autom. privater Chat mit dem User)
        subscriber.forEach(user -> {
            SendMessage sendMessage = new SendMessage(user.id, sendingMessageText);
            BaseResponse sendMessageResponse = senderBot.execute(sendMessage);
            if (!sendMessageResponse.isOk()) {
                log.error(String.format("Error while sending message to user %d: %d - %s", user.id, sendMessageResponse.errorCode(), sendMessageResponse.description()));
            }
        });
    }

    private void confirmSendMessage(TelegramBot adminBot, long chatId, String finalMessageText) {
        // Benutzer um Bestätigung bitten, dass die Nachricht gesendet werden soll

        // Bestätigungstext ausgeben
        String confirmMessageText = texts.findById("CONFIRM_SEND_MESSAGE").get().text;
        SendMessage confirmMessage = new SendMessage(chatId, confirmMessageText);
        BaseResponse confirmMessageResponse = adminBot.execute(confirmMessage);

        if (!confirmMessageResponse.isOk()) {
            log.error(String.format("Error while sending confirm message to moderator / admin: %d - %s", confirmMessageResponse.errorCode(), confirmMessageResponse.description()));
        }

        // Bestätigungsbutton erstellen
        String confirmText = texts.findById("CONFIRM_BUTTON").get().text;
        String declineText = texts.findById("DECLINE_BUTTON").get().text;

        InlineKeyboardButton[] keyboardButtons = new InlineKeyboardButton[2];
        keyboardButtons[0] = new InlineKeyboardButton(confirmText).callbackData("true");
        keyboardButtons[1] = new InlineKeyboardButton(declineText).callbackData("false");

        // Finale Nachricht ausgeben mit reply_markup
        SendMessage finalMessage = new SendMessage(chatId, finalMessageText).replyMarkup(new InlineKeyboardMarkup(keyboardButtons));
        BaseResponse finalMessageResponse = adminBot.execute(finalMessage);

        if (!finalMessageResponse.isOk()) {
            log.error(String.format("Error while sending final message to moderator / admin: %d - %s", finalMessageResponse.errorCode(), finalMessageResponse.description()));
        }
    }

    private void confirmMessageCategory(TelegramBot adminBot, long chatId, int messageId, String callbackData) {
        // Kategorie laden
        Category selectedCategory = categoryRepository.findById(Integer.parseInt(callbackData)).get();

        // Nachricht aktualisieren
        String updateMessageText = texts.findById("CONFIRM_MESSAGE_CATEGORY").get().text;
        EditMessageText editMessageText = new EditMessageText(chatId, messageId, String.format(updateMessageText, selectedCategory.description));
        BaseResponse messageChangeResponse = adminBot.execute(editMessageText);

        if (!messageChangeResponse.isOk()) {
            log.error("Error while changing 'send_message' message: " + messageChangeResponse.errorCode() + " - " + messageChangeResponse.description());
        }
    }

    private void sendMessageCategories(TelegramBot adminBot, long chatId) {
        // Kategorien ermitteln
        List<Category> categories = categoryRepository.findAll();

        // Button mit Text erstellen
        List<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();

        categories.forEach(category -> inlineKeyboardButtons.add(new InlineKeyboardButton(category.description).callbackData(category.id.toString())));

        // Button Liste zu Array konvertieren
        InlineKeyboardButton[] keyboardButtons = new InlineKeyboardButton[inlineKeyboardButtons.size()];
        keyboardButtons = inlineKeyboardButtons.toArray(keyboardButtons);

        // Neue Nachricht senden
        // Buttons zurückgeben
        String categorySelectionText = texts.findById("MESSAGE_CATEGORY_SELECTION").get().text;
        SendMessage sendMessage = new SendMessage(chatId, categorySelectionText);

        // Button zum Call hinzufügen
        sendMessage.replyMarkup(new InlineKeyboardMarkup(keyboardButtons));

        // Senden
        BaseResponse messageResponse = adminBot.execute(sendMessage);

        // Fehler Logging
        if (!messageResponse.isOk()) {
            log.error(messageResponse.errorCode() + " - " + messageResponse.description());
        }
    }
}

package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Contact;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminMenu {
    private static final Logger log = LogManager.getLogger(AdminMenu.class);

    private AdminChatRepository adminChatRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;
    private TextRepository texts;
    private CounterRepository counterRepository;
    private ModeratorRepository moderatorRepository;

    @Autowired
    public AdminMenu(AdminChatRepository adminChatRepository, CategoryRepository categoryRepository, UserRepository userRepository, TextRepository texts, CounterRepository counterRepository, ModeratorRepository moderatorRepository) {
        this.adminChatRepository = adminChatRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.texts = texts;
        this.counterRepository = counterRepository;
        this.moderatorRepository = moderatorRepository;
    }

    public void processUpdate(Update update, String adminBotToken, String senderBotToken) {
        // Bots erstellen
        TelegramBot adminBot = new TelegramBot(adminBotToken);
        TelegramBot senderBot = new TelegramBot(senderBotToken);
        try {
            // Normale Nachricht / Kommando
            if (update.message() != null) {
                long chatId = update.message().chat().id();
                int messageId = update.message().messageId();

                // Prüfe ob User Moderator bzw. berechtigt ist
                if (userIsNotModerator(update.message().from().id())) {
                    showUnauthorizedMessage(adminBot, chatId);
                    return;
                }

                // Chat Statusabfrage
                Optional<AdminChat> optionalCurrentChat = adminChatRepository.findById(chatId);

                // Prüfe ob Nachricht Kontakt enthält
                if (update.message().contact() != null) {
                    // Prüfe ob Chat Status existiert
                    if (optionalCurrentChat.isPresent()) {
                        AdminChat newModeratorChat = optionalCurrentChat.get();

                        if (newModeratorChat.status.equals("moderator_administration")) {
                            addNewModerator(adminBot, chatId, update.message().contact());
                            showModeratorMenu(adminBot, chatId, null);
                        }
                    }
                }

                // Prüfe ob Nachricht Text enthält
                if (update.message().text() != null) {

                    // Prüfe auf abbrechen
                    if (update.message().text().equals("/abbrechen")) {
                        // Status entfernen und Antwort senden
                        adminChatRepository.deleteById(chatId);
                        String cancelMessageText = texts.findById("CANCEL").get().text;
                        SendMessage cancelMessage = new SendMessage(chatId, cancelMessageText);
                        adminBot.execute(cancelMessage);
                        return;
                    }

                    // Unterscheidung nach Chat Status
                    if (optionalCurrentChat.isPresent()) {
                        // Chat besitzt Status, prüfen und verarbeiten
                        AdminChat currentChat = optionalCurrentChat.get();

                        switch (currentChat.status) {
                            case "confirm":
                                confirmSendMessage(adminBot, chatId, update.message().text());
                                return;
                            case "categories":
                                // Eingehende Antwort löschen
                                deleteMessageFromChat(adminBot, chatId, messageId);

                                // Neue Kategorien eingegeben, parsen und hinzufügen
                                addNewCategories(adminBot, chatId, update.message().text());
                                return;

                            case "category;change":
                                // Kategorie zwischenspeichern
                                int category = currentChat.category;

                                // Status wieder zurücksetzen
                                currentChat.status = "categories";
                                currentChat.category = null;
                                adminChatRepository.save(currentChat);

                                // Neue Kategoriebezeichnung übernehmen
                                setNewCategoryDescription(category, update.message().text());

                                // Menü ausgeben
                                showChangeOrDeleteCategoryMenu(adminBot, chatId, null, false);
                                return;

                            default:
                                // Chat löschen, whatever happend...
                                adminChatRepository.deleteById(chatId);

                                // Logmeldung ausgeben
                                log.warn(String.format("UNICORN Error - Unknown chat status: %s", currentChat.status));

                                // Benutzer benachrichtigen
                                SendMessage unicornError = new SendMessage(chatId, texts.findById("UNICORN_ERROR").get().text);
                                BaseResponse unicornErrorResponse = adminBot.execute(unicornError);
                                if (!unicornErrorResponse.isOk()) {
                                    log.error("Error while sending UNICORN error message: %d - %s: " + unicornErrorResponse.errorCode() + " - " + unicornErrorResponse.description());
                                }
                                return;
                        }
                    } else {
                        // Kein gespeicherter Chat Status, neu Beginnen:
                        // Chat Status setzen
                        AdminChat currentChat = new AdminChat(chatId);

                        switch (update.message().text()) {
                            case "/start":
                                sendWelcomeMessage(adminBot, chatId, update.message().from().firstName());
                                return;

                            case "/kategorien":
                                // Chat Status setzen
                                currentChat.status = "categories";
                                adminChatRepository.save(currentChat);

                                // Menü anzeigen (neu)
                                showCategoriesMenu(adminBot, chatId, null);
                                return;

                            case "/nachricht":
                                // Chat Status setzen
                                currentChat.status = "message";
                                adminChatRepository.save(currentChat);

                                // Nachricht ausgeben mit Kategorien
                                sendMessageCategories(adminBot, chatId);
                                return;

                            case "/moderatoren":
                                // Bot Administrieren -> Moderatoren / Admin Berechtigung

                                // Berechtigung prüfen, ist Moderator Admin?
                                if (userIsAdministrator(update.message().from().id())) {
                                    // Moderator Administration anzeigen
                                    currentChat.status = "moderator_administration";
                                    adminChatRepository.save(currentChat);
                                    showModeratorMenu(adminBot, chatId, null);
                                } else {
                                    showUnauthorizedMessage(adminBot, chatId);
                                }
                                return;

                            default:
                                log.warn(String.format("Unknown command: %s", update.message().text()));
                                String unknownCommandText = texts.findById("UNKNOWN_COMMAND").get().text;
                                SendMessage unknownCommandMessage = new SendMessage(chatId, unknownCommandText);
                                BaseResponse unknownCommandResponse = adminBot.execute(unknownCommandMessage);

                                if (!unknownCommandResponse.isOk()) {
                                    log.error("Error while sending unknown command response to user: " + unknownCommandResponse.errorCode() + " - " + unknownCommandResponse.description());
                                }

                                // Chat aufräumen, zur Vorsicht
                                adminChatRepository.deleteById(chatId);
                                return;
                        }
                    }
                }
            }

            // Ergebnis der Buttons
            if (update.callbackQuery() != null) {
                long chatId = update.callbackQuery().message().chat().id();
                String data = update.callbackQuery().data();
                int messageId = update.callbackQuery().message().messageId();

                // Prüfe ob User Moderator bzw. berechtigt ist
                if (userIsNotModerator(update.callbackQuery().from().id())) {
                    showUnauthorizedMessage(adminBot, chatId);
                }

                // Chat Status abrufen
                Optional<AdminChat> optionalChat = adminChatRepository.findById(chatId);
                AdminChat currentChat = new AdminChat();
                if (optionalChat.isPresent()) {
                    currentChat = optionalChat.get();
                }

                switch (currentChat.status) {
                    case "message":
                        // Abbrechen abfangen
                        if (data.equals("return")) {
                            // Chat-Status löschen
                            adminChatRepository.deleteById(chatId);

                            // Nachrichtenversand abgebrochen
                            EditMessageText editSendMessageText = new EditMessageText(chatId, messageId, texts.findById("CONFIRM_CANCEL").get().text);
                            BaseResponse response = adminBot.execute(editSendMessageText);

                            if (!response.isOk()) {
                                log.error(String.format("Error while confirming send message cancel: %d - %s", response.errorCode(), response.description()));
                            }
                            return;
                        }

                        // Kategorie in AdminChat speichern, neuer Status
                        currentChat.category = Integer.parseInt(data);
                        currentChat.status = "confirm";
                        adminChatRepository.save(currentChat);

                        // Kategorie bestätigen, Aufforderung Nachrichteneingabe
                        confirmMessageCategory(adminBot, chatId, messageId, data);
                        return;

                    case "confirm":
                        // Chat Status löschen, egal was passiert
                        adminChatRepository.deleteById(chatId);

                        // Unterscheiden ob bestätigt oder abgebrochen wurde
                        if (Boolean.parseBoolean(data)) {
                            // Buttons deaktivieren
                            // ReplyMarkup der Original-Nachricht ändern
                            EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup(chatId, messageId).replyMarkup(new InlineKeyboardMarkup());
                            BaseResponse editMarkupResponse = adminBot.execute(editMessageReplyMarkup);

                            if (!editMarkupResponse.isOk()) {
                                log.error("Error while changing 'confirm_message' reply_markup: " + editMarkupResponse.errorCode() + " - " + editMarkupResponse.description());
                            }

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
                        return;

                    case "categories":
                        String[] returnQuery = data.split(";");
                        String type = returnQuery[0];
                        String action = returnQuery[1];

                        // Menü Aufbau durchgehen
                        switch (type) {
                            // Menü-Aktion
                            case "menu":
                                switch (action) {
                                    // Zurück
                                    case "return":
                                        // Chat löschen / Status zurücksetzen
                                        adminChatRepository.deleteById(chatId);
                                        showAdministrationFinishedText(adminBot, chatId, messageId);
                                        return;

                                    // Erstellen Menü anzeigen
                                    case "create":
                                        showCreateCategoryMenu(adminBot, chatId, messageId);
                                        return;

                                    // Ändern Menü anzeigen
                                    case "change":
                                        showChangeOrDeleteCategoryMenu(adminBot, chatId, messageId, false);
                                        return;

                                    // Löschen Menü anzeigen
                                    case "delete":
                                        showChangeOrDeleteCategoryMenu(adminBot, chatId, messageId, true);
                                        return;
                                }
                                return;

                            // Erstell-Aktion
                            case "create":
                                showCategoriesMenu(adminBot, chatId, messageId);
                                return;

                            // Ändern-Aktion
                            case "change":
                                if ("return".equals(action)) {
                                    // Zurück
                                    showCategoriesMenu(adminBot, chatId, messageId);
                                    return;
                                }

                                // Eingabeaufforderung anzeigen
                                currentChat.status = "category;change";
                                currentChat.category = Integer.parseInt(action);
                                adminChatRepository.save(currentChat);
                                showEnterNewCategoryDescriptionText(adminBot, chatId, messageId, currentChat.category);
                                return;

                            // Löschen-Aktion
                            case "delete":
                                // Zurück
                                if ("return".equals(action)) {
                                    showCategoriesMenu(adminBot, chatId, messageId);
                                    return;
                                }

                                // Kategorie löschen
                                deleteCategory(adminBot, senderBot, chatId, action);

                                // Menü wieder anzeigen
                                showChangeOrDeleteCategoryMenu(adminBot, chatId, null, true);
                                return;
                        }


                        return;

                    case "moderator_administration":
                        String[] moderatorReturnQuery = data.split(";");
                        String moderatorType = moderatorReturnQuery[0];
                        String moderatorAction = moderatorReturnQuery[1];

                        switch (moderatorType) {
                            case "menu":
                                switch (moderatorAction) {
                                    // Zurück
                                    case "return":
                                        // Chat löschen
                                        adminChatRepository.deleteById(chatId);
                                        showAdministrationFinishedText(adminBot, chatId, messageId);
                                        return;
                                    case "add":
                                        // Hinzufügen Menü anzeigen
                                        showAddModeratorMenu(adminBot, chatId, messageId);
                                        return;
                                    case "change":
                                        // Moderator ändern Menü anzeigen
                                        showChangeOrDeleteModeratorMenu(adminBot, chatId, messageId, false);
                                        return;
                                    case "delete":
                                        // Moderator entfernen Menü anzeigen
                                        showChangeOrDeleteModeratorMenu(adminBot, chatId, messageId, true);
                                        return;
                                }
                                return;

                            case "change":
                                // Moderatorenberechtigung ändern
                                if ("return".equals(moderatorAction)) {
                                    // Zurück
                                    showModeratorMenu(adminBot, chatId, messageId);
                                    return;
                                }

                                // Moderator umschalten
                                switchModeratorType(moderatorAction);
                                // Ändern Menü anzeigen (false)
                                showChangeOrDeleteModeratorMenu(adminBot, chatId, messageId, false);
                                return;

                            case "delete":
                                // Moderator entfernen
                                if ("return".equals(moderatorAction)) {
                                    // Zurück
                                    showModeratorMenu(adminBot, chatId, messageId);
                                    return;
                                }

                                // Moderator löschen
                                deleteModerator(adminBot, chatId, messageId, moderatorAction);
                                // Automatisch zurückkehren, neue Nachricht
                                showModeratorMenu(adminBot, chatId, null);
                                return;
                        }
                        return;

                    default:
                        log.warn(String.format("Unknown query result %s to status %s", data, currentChat.status));
                        SendMessage sendUnknownQueryResultMessage = new SendMessage(currentChat.id, String.format(texts.findById("DUCK_ERROR").get().text, data, currentChat.status));

                        BaseResponse response = adminBot.execute(sendUnknownQueryResultMessage);
                        if (!response.isOk()) {
                            log.error(String.format("Errror while sending unknown_query_result_message / DUCK: %d - %s", response.errorCode(), response.description()));
                        }

                        // Chat aufräumen, zur Vorsicht
                        adminChatRepository.deleteById(chatId);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());

            if (update.message() != null) {
                SendMessage sendUnknownQueryResultMessage = new SendMessage(update.message().chat().id(), texts.findById("SQUIRREL_ERROR").get().text);

                BaseResponse response = adminBot.execute(sendUnknownQueryResultMessage);
                if (!response.isOk()) {
                    log.error(String.format("Errror while sending exception occured error / SQUIRREL: %d - %s", response.errorCode(), response.description()));
                }
            }


        }
    }

    private void deleteCategory(TelegramBot adminBot, TelegramBot senderBot, long adminChatId, String categoryStringId) {
        // Kategorie-ID parsen
        int categoryId = Integer.parseInt(categoryStringId);
        Category category = categoryRepository.findById(categoryId).get();
        categoryRepository.deleteById(categoryId);

        // Alle Benutzer ermitteln die die Kategorie abonniert haben und Benachrichtigen
        List<User> users = userRepository.findAllByCategory(categoryId);

        String userMessageText = texts.findById("SUBSCRIBER_CATEGORY_REMOVED").get().text;
        for (User user : users) {
            // Kategorie aus User-Profil entfernen
            user.categories.removeIf(id -> id == categoryId);
            userRepository.save(user);

            // User benachrichtigen
            SendMessage sendUserMessage = new SendMessage(user.id, String.format(userMessageText, user.name, category.description));
            BaseResponse userMessageResponse = senderBot.execute(sendUserMessage);

            if (!userMessageResponse.isOk()) {
                log.error(String.format("Error while sending category_deleted to subscriber %d | %s for category %d | %s", user.id, user.name, category.id, category.description));
            }
        }

        // Admin benachrichtigen
        SendMessage categoryRemovedMessage = new SendMessage(adminChatId, String.format(texts.findById("CONFIRM_CATEGORY_REMOVED").get().text, category.description, users.size()));
        BaseResponse adminResponse = adminBot.execute(categoryRemovedMessage);

        if (!adminResponse.isOk()) {
            log.error(String.format("Error while sending confirm_category_removed: %d - %s", adminResponse.errorCode(), adminResponse.description()));
        }
    }

    private void addNewModerator(TelegramBot adminBot, long chatId, Contact contact) {
        Moderator moderator = new Moderator(contact.userId(), contact.firstName());
        moderatorRepository.save(moderator);

        // Bestätigung senden
        String messageText = String.format(texts.findById("CONFIRM_NEW_MODERATOR").get().text, moderator.firstName);
        SendMessage sendMessage = new SendMessage(chatId, messageText);

        BaseResponse response = adminBot.execute(sendMessage);

        if (!response.isOk()) {
            log.error(String.format("Errror while sending confirm_new_moderator: %d - %s", response.errorCode(), response.description()));
        }
    }

    private void showAddModeratorMenu(TelegramBot adminBot, long chatId, int lastMessageId) {
        EditMessageText editMessageText = new EditMessageText(chatId, lastMessageId, texts.findById("ADD_MODERATOR_MENU_TEXT").get().text);
        BaseResponse response = adminBot.execute(editMessageText);

        if (!response.isOk()) {
            log.error(String.format("Errror while sending confirm_delete_moderator: %d - %s", response.errorCode(), response.description()));
        }
    }

    private void deleteModerator(TelegramBot adminBot, long chatId, int messageId, String moderatorId) {
        // Moderator Daten abrufen
        int userId = Integer.parseInt(moderatorId);
        Moderator moderator = moderatorRepository.findById(userId).get();

        // Moderator löschen
        moderatorRepository.deleteById(userId);

        // Bestätigen
        String messageText = String.format(texts.findById("MODERATOR_DELETED").get().text, moderator.firstName);
        EditMessageText editMessageText = new EditMessageText(chatId, messageId, messageText);
        BaseResponse response = adminBot.execute(editMessageText);

        if (!response.isOk()) {
            log.error(String.format("Errror while sending confirm_delete_moderator: %d - %s", response.errorCode(), response.description()));
        }
    }

    private void showChangeOrDeleteModeratorMenu(TelegramBot adminBot, long chatId, Integer lastMessageId, boolean delete) {
        // Aktion definieren
        String action = delete ? "delete;" : "change;";
        // Moderatoren ermitteln
        List<Moderator> moderators = moderatorRepository.findAll();

        // Buttons mit Text erstellen, jeweils neue Zeile + Zurück Button
        InlineKeyboardButton[][] buttons = new InlineKeyboardButton[moderators.size() + 1][];

        for (int i = 0; i < moderators.size(); i++) {
            Moderator moderator = moderators.get(i);
            // Button text abhängig davon oder Moderator Admin ist
            String buttonText = moderator.isAdministrator ? String.format(texts.findById("ADMIN_SUFFIX").get().text, moderator.firstName) : moderator.firstName;
            buttons[i] = new InlineKeyboardButton[]{new InlineKeyboardButton(buttonText).callbackData(action + moderator.id)};
        }

        // Zurück Button hinzufügen
        buttons[buttons.length - 1] = new InlineKeyboardButton[]{new InlineKeyboardButton(texts.findById("BACK_ADMINISTRATION_BUTTON").get().text).callbackData(action + "return")};

        // Inline Markup erstellen
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);

        // Nachricht aktualisieren / Neu senden
        BaseResponse response;
        String messageText = delete ? texts.findById("DELETE_MODERATOR_ADMINISTRATION_TEXT").get().text : texts.findById("CHANGE_MODERATOR_ADMINISTRATION_TEXT").get().text;
        if (lastMessageId == null) {
            SendMessage sendMessage = new SendMessage(chatId, messageText).replyMarkup(markup);
            response = adminBot.execute(sendMessage);
        } else {
            EditMessageText editMessage = new EditMessageText(chatId, lastMessageId, messageText).replyMarkup(markup);
            response = adminBot.execute(editMessage);
        }

        if (!response.isOk()) {
            log.error(String.format("Errror while sending show_change_or_delete_moderator_menu, delete: %s, %d - %s", delete, response.errorCode(), response.description()));
        }
    }

    private void switchModeratorType(String moderatorStringId) {
        int moderatorId = Integer.parseInt(moderatorStringId);
        Moderator moderator = moderatorRepository.findById(moderatorId).get();
        moderator.isAdministrator = !moderator.isAdministrator;

        // Neuen Status speichern
        moderatorRepository.save(moderator);
    }

    private void showModeratorMenu(TelegramBot adminBot, long chatId, Integer lastMessageId) {
        // Administrationsmenü angezigen
        // Menu anzeigen (Erstellen, Ändern, Löschen, Beenden)
        InlineKeyboardButton[] rowOneButtons = new InlineKeyboardButton[1];
        InlineKeyboardButton[] rowTwoButtons = new InlineKeyboardButton[1];
        InlineKeyboardButton[] rowThreeButtons = new InlineKeyboardButton[1];
        InlineKeyboardButton[] rowFourButtons = new InlineKeyboardButton[1];

        // Hinzufügen
        rowOneButtons[0] = new InlineKeyboardButton(texts.findById("ADD_MODERATOR_BUTTON").get().text).callbackData("menu;add");
        // Ändern
        rowTwoButtons[0] = new InlineKeyboardButton(texts.findById("CHANGE_MODERATOR_BUTTON").get().text).callbackData("menu;change");
        // Entfernen
        rowThreeButtons[0] = new InlineKeyboardButton(texts.findById("DELETE_MODERATOR_BUTTON").get().text).callbackData("menu;delete");
        // Beenden
        rowFourButtons[0] = new InlineKeyboardButton(texts.findById("CLOSE_ADMINISTRATION_BUTTON").get().text).callbackData("menu;return");

        BaseResponse response;
        if (lastMessageId == null) {
            SendMessage categoryAdministrationMessage = new SendMessage(chatId, texts.findById("MODERATOR_ADMINISTRATION").get().text).replyMarkup(new InlineKeyboardMarkup(rowOneButtons, rowTwoButtons, rowThreeButtons, rowFourButtons));
            response = adminBot.execute(categoryAdministrationMessage);
        } else {
            EditMessageText categoryAdministrationMessage = new EditMessageText(chatId, lastMessageId, texts.findById("MODERATOR_ADMINISTRATION").get().text).replyMarkup(new InlineKeyboardMarkup(rowOneButtons, rowTwoButtons, rowThreeButtons, rowFourButtons));
            response = adminBot.execute(categoryAdministrationMessage);
        }

        if (!response.isOk()) {
            log.error(String.format("Errror while sending moderator_administration: %d - %s", response.errorCode(), response.description()));
        }
    }

    private void showUnauthorizedMessage(TelegramBot adminBot, long chatId) {
        SendMessage sendMessage = new SendMessage(chatId, texts.findById("UNAUTHORIZED_MESSAGE").get().text);
        BaseResponse response = adminBot.execute(sendMessage);

        if (!response.isOk()) {
            log.error(String.format("Errror while sending unauthorized_message: %d - %s", response.errorCode(), response.description()));
        }
    }

    private boolean userIsNotModerator(Integer id) {
        // Prüft ob die Benutzer-ID als Moderator hinterlegt ist
        return moderatorRepository.findById(id).isEmpty();
    }

    private boolean userIsAdministrator(Integer id) {
        // Prüft ob die Benutzer-ID als Moderator hinterlegt ist und Admin-Flag gesetzt ist
        Optional<Moderator> moderator = moderatorRepository.findById(id);
        return moderator.map(value -> value.isAdministrator).orElse(false);
    }

    private void showAdministrationFinishedText(TelegramBot adminBot, long chatId, int messageId) {
        EditMessageText editMessage = new EditMessageText(chatId, messageId, texts.findById("ADMINISTRATION_FINISHED_TEXT").get().text);
        BaseResponse response = adminBot.execute(editMessage);


        if (!response.isOk()) {
            log.error(String.format("Errror while sending show_enter_new_category_description_text: %d - %s", response.errorCode(), response.description()));
        }
    }

    private void showEnterNewCategoryDescriptionText(TelegramBot adminBot, long chatId, Integer messageId, int categoryId) {
        String messageText = String.format(texts.findById("CHANGE_CATEGORY_NEW_DESCRIPTION_TEXT").get().text, categoryRepository.findById(categoryId).get().description);

        BaseResponse response;
        if (messageId == null) {
            SendMessage sendMessage = new SendMessage(chatId, messageText);
            response = adminBot.execute(sendMessage);
        } else {
            EditMessageText editMessage = new EditMessageText(chatId, messageId, messageText);
            response = adminBot.execute(editMessage);
        }


        if (!response.isOk()) {
            log.error(String.format("Errror while sending show_enter_new_category_description_text: %d - %s", response.errorCode(), response.description()));
        }
    }

    private void setNewCategoryDescription(int category, String newDescription) {
        // Geänderte Kategorie speichern
        categoryRepository.save(new Category(category, newDescription));
    }

    private void deleteMessageFromChat(TelegramBot adminBot, long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
        BaseResponse deleteMessageResponse = adminBot.execute(deleteMessage);
        if (!deleteMessageResponse.isOk()) {
            log.error(String.format("Errror while deleting message with id %d: %d - %s", messageId, deleteMessageResponse.errorCode(), deleteMessageResponse.description()));
        }
    }

    private void showChangeOrDeleteCategoryMenu(TelegramBot adminBot, long chatId, Integer messageId, boolean delete) {
        // Callback Data bestimmen
        String callbackPrefix = delete ? "delete;" : "change;";

        // Kategorien ermitteln
        List<Category> categories = categoryRepository.findAll();

        // Buttons mit Text erstellen, jeweils neue Zeile + Zurück Button
        InlineKeyboardButton[][] buttons = new InlineKeyboardButton[categories.size() + 1][];

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            buttons[i] = new InlineKeyboardButton[]{new InlineKeyboardButton(category.description).callbackData(callbackPrefix + category.id)};
        }

        // Zurück Button hinzufügen
        buttons[buttons.length - 1] = new InlineKeyboardButton[]{new InlineKeyboardButton(texts.findById("BACK_ADMINISTRATION_BUTTON").get().text).callbackData(callbackPrefix + "return")};

        // Inline Markup erstellen
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);

        // Nachricht aktualisieren / Neu senden
        BaseResponse response;
        String messageText = delete ? texts.findById("DELETE_CATEGORY_ADMINISTRATION_TEXT").get().text : texts.findById("CHANGE_CATEGORY_ADMINISTRATION_TEXT").get().text;
        if (messageId == null) {
            SendMessage sendMessage = new SendMessage(chatId, messageText).replyMarkup(markup);
            response = adminBot.execute(sendMessage);
        } else {
            EditMessageText editMessage = new EditMessageText(chatId, messageId, messageText).replyMarkup(markup);
            response = adminBot.execute(editMessage);
        }

        if (!response.isOk()) {
            log.error(String.format("Errror while sending show_change_category_menu: %d - %s", response.errorCode(), response.description()));
        }
    }

    private void addNewCategories(TelegramBot adminBot, long chatId, String text) {
        // Pro Zeile eine neue Kategorie (Telegram nuzt linefeed, nicht carriage return)
        String[] categories = text.split("\n");

        for (String category : categories) {
            categoryRepository.insert(newAutoincrementedCategory(category));
        }

        // Aktualisiertes Menü ausgeben
        showCreateCategoryMenu(adminBot, chatId, null);
    }

    private Category newAutoincrementedCategory(String categoryDescription) {
        // Da es in MongoDB kein automatisiertes AUTO_INCREMENT gibt hier manuell
        Optional<Counter> optionalCounter = counterRepository.findById("categories");
        Counter counter;

        if (optionalCounter.isEmpty()) {
            counter = new Counter("categories");
        } else {
            counter = optionalCounter.get();
            if (counter.sequenceValue == null) {
                counter.sequenceValue = 0;
            } else {
                counter.sequenceValue += 1;
            }
        }

        counterRepository.save(counter);

        return new Category(counter.sequenceValue, categoryDescription);
    }

    private void showCreateCategoryMenu(TelegramBot adminBot, long chatId, Integer messageId) {

        // Nur Zurück-Button
        InlineKeyboardButton[] buttons = new InlineKeyboardButton[1];
        String returnText = texts.findById("BACK_ADMINISTRATION_BUTTON").get().text;
        buttons[0] = new InlineKeyboardButton(returnText).callbackData("create;return");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(buttons);

        // Nachricht ändern und Kategorien anzeigen
        StringBuilder availableCategoriesMessageText = new StringBuilder(texts.findById("CURRENT_CATEGORIES").get().text);

        // Kategorien auslesen
        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {
            availableCategoriesMessageText.append("\n").append(category.id).append("-").append(category.description);
        }

        BaseResponse availableCategoriesResponse;
        if (messageId == null) {
            SendMessage availableCategories = new SendMessage(chatId, availableCategoriesMessageText.toString()).replyMarkup(inlineKeyboardMarkup);
            availableCategoriesResponse = adminBot.execute(availableCategories);
        } else {
            EditMessageText availableCategories = new EditMessageText(chatId, messageId, availableCategoriesMessageText.toString()).replyMarkup(inlineKeyboardMarkup);
            availableCategoriesResponse = adminBot.execute(availableCategories);
        }

        if (!availableCategoriesResponse.isOk()) {
            log.error(String.format("Errror while sending category_administration_create_message: %d - %s", availableCategoriesResponse.errorCode(), availableCategoriesResponse.description()));
        }
    }

    private void showCategoriesMenu(TelegramBot adminBot, long chatId, Integer messageId) {
        // Menu anzeigen (Erstellen, Ändern, Löschen, Beenden)
        InlineKeyboardButton[] rowOneButtons = new InlineKeyboardButton[2];
        InlineKeyboardButton[] rowTwoButtons = new InlineKeyboardButton[2];

        // Hinzufügen
        rowOneButtons[0] = new InlineKeyboardButton(texts.findById("CREATE_CATEGORY_ADMINISTRATION_BUTTON").get().text).callbackData("menu;create");
        // Ändern
        rowOneButtons[1] = new InlineKeyboardButton(texts.findById("CHANGE_CATEGORY_ADMINISTRATION_BUTTON").get().text).callbackData("menu;change");
        // Beenden
        rowTwoButtons[0] = new InlineKeyboardButton(texts.findById("CLOSE_ADMINISTRATION_BUTTON").get().text).callbackData("menu;return");
        // Entfernen
        rowTwoButtons[1] = new InlineKeyboardButton(texts.findById("DELETE_CATEGORY_ADMINISTRATION_BUTTON").get().text).callbackData("menu;delete");

        BaseResponse response;
        if (messageId == null) {
            SendMessage categoryAdministrationMessage = new SendMessage(chatId, texts.findById("CATEGORY_ADMINISTRATION").get().text).replyMarkup(new InlineKeyboardMarkup(rowOneButtons, rowTwoButtons));
            response = adminBot.execute(categoryAdministrationMessage);
        } else {
            EditMessageText categoryAdministrationMessage = new EditMessageText(chatId, messageId, texts.findById("CATEGORY_ADMINISTRATION").get().text).replyMarkup(new InlineKeyboardMarkup(rowOneButtons, rowTwoButtons));
            response = adminBot.execute(categoryAdministrationMessage);
        }

        if (!response.isOk()) {
            log.error(String.format("Errror while sending category_administration: %d - %s", response.errorCode(), response.description()));
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

        // Buttons mit Text erstellen, jeweils neue Zeile + Zurück Button
        InlineKeyboardButton[][] buttons = new InlineKeyboardButton[categories.size() + 1][];

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            buttons[i] = new InlineKeyboardButton[]{new InlineKeyboardButton(category.description).callbackData(category.id.toString())};
        }

        // Zurück Button hinzufügen
        buttons[buttons.length - 1] = new InlineKeyboardButton[]{new InlineKeyboardButton(texts.findById("BACK_ADMINISTRATION_BUTTON").get().text).callbackData("return")};

        // Inline Markup erstellen
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);

        // Neue Nachricht senden
        // Buttons zurückgeben
        String categorySelectionText = texts.findById("MESSAGE_CATEGORY_SELECTION").get().text;
        SendMessage sendMessage = new SendMessage(chatId, categorySelectionText);

        // Button zum Call hinzufügen
        sendMessage.replyMarkup(markup);

        // Senden
        BaseResponse messageResponse = adminBot.execute(sendMessage);

        // Fehler Logging
        if (!messageResponse.isOk()) {
            log.error(messageResponse.errorCode() + " - " + messageResponse.description());
        }
    }
}

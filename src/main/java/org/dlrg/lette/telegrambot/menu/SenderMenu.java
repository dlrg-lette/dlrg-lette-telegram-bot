package org.dlrg.lette.telegrambot.menu;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dlrg.lette.telegrambot.data.Chat;
import org.dlrg.lette.telegrambot.data.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class SenderMenu {
    private static final Logger log = LogManager.getLogger(SenderMenu.class);

    @Autowired
    private static ChatRepository chatRepository;

    public static void processUpdate(Update update, String senderBotToken) {
        TelegramBot adminBot = new TelegramBot(senderBotToken);

        // Prüfen ob normale Nachricht / Kommando oder Inline-Query Result
        if (update.message() != null) {
            if (update.message().text().equals("/inline")) {
                SendMessage sendMessage = new SendMessage(update.message().chat().id(), "Test inline Buttons...");
                sendMessage.replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton[]{new InlineKeyboardButton("Button 1").callbackData("1"), new InlineKeyboardButton("Button 2").callbackData("2")}));

                BaseResponse inlineMessage = adminBot.execute(sendMessage);

                if (!inlineMessage.isOk()) {
                    log.error(String.format("%d - %s", inlineMessage.errorCode(), inlineMessage.description()));
                } else {
                    log.info("Mongo Entry exists: " + chatRepository.existsById(update.message().chat().id()));

                    log.info("Chat ID: " + update.message().chat().id());
                    Chat newChat = new Chat("status", "message");
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

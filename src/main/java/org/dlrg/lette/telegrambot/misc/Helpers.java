package org.dlrg.lette.telegrambot.misc;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;

public class Helpers {

    // Format incoming Update-Message with HTML
    public static String formatMessageText(Message message) {
        StringBuilder stringBuilder = new StringBuilder();
        String plainMessage = message.text();


        if (message.entities() != null) {
            // Go through all entities
            MessageEntity[] entities = message.entities();
            int lastEntityIndex = 0;
            for (int i = 0, entitiesLength = entities.length; i < entitiesLength; i++) {
                MessageEntity entity = entities[i];

                // Default, get characters from last entity up to offset
                stringBuilder.append(plainMessage, lastEntityIndex, entity.offset());

                // Update last entity index
                lastEntityIndex = entity.offset() + entity.length();

                // Format with HTML
                switch (entity.type()) {
                    case bold:
                        stringBuilder.append("<b>");
                        stringBuilder.append(escapeString(plainMessage.substring(entity.offset(), lastEntityIndex)));
                        stringBuilder.append("</b>");
                        break;

                    case italic:
                        stringBuilder.append("<i>");
                        stringBuilder.append(escapeString(plainMessage.substring(entity.offset(), lastEntityIndex)));
                        stringBuilder.append("</i>");
                        break;

                    case underline:
                        stringBuilder.append("<u>");
                        stringBuilder.append(escapeString(plainMessage.substring(entity.offset(), lastEntityIndex)));
                        stringBuilder.append("</u>");
                        break;

                    case strikethrough:
                        stringBuilder.append("<s>");
                        stringBuilder.append(escapeString(plainMessage.substring(entity.offset(), lastEntityIndex)));
                        stringBuilder.append("</s>");
                        break;

                    case code:
                        stringBuilder.append("<code>");
                        stringBuilder.append(escapeString(plainMessage.substring(entity.offset(), lastEntityIndex)));
                        stringBuilder.append("</code>");
                        break;

                    case pre:
                        stringBuilder.append("<pre>");
                        stringBuilder.append(escapeString(plainMessage.substring(entity.offset(), lastEntityIndex)));
                        stringBuilder.append("</pre>");
                        break;

                    case url:
                        stringBuilder.append("<a href=\"").append(entity.url()).append("\">");
                        stringBuilder.append(escapeString(plainMessage.substring(entity.offset(), lastEntityIndex)));
                        stringBuilder.append("</a>");
                        break;

                    default:
                        // Not supported entity type, use plain text
                        stringBuilder.append(plainMessage, entity.offset(), lastEntityIndex);
                }

                // Add trailing characters after last element to complete message
                if (i == (entitiesLength - 1)) {
                    stringBuilder.append(plainMessage, lastEntityIndex, plainMessage.length());
                }
            }
        } else {
            // No text entities available, use text as it is
            stringBuilder.append(message.text());
        }

        // Return formatted String
        return stringBuilder.toString();
    }

    // Escape characters for use in HTML
    public static String escapeString(String text) {
        text = text.replaceAll("&", "&amp;");
        text = text.replaceAll("<", "&lt;");
        text = text.replaceAll(">", "&gt;");
        return text;
    }
}

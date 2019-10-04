package org.dlrg.lette.telegrambot.data;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "senderChat")
public class SenderChat extends Chat {

    public SenderChat() {
        super();
    }

    public SenderChat(Long id, String status) {
        super(id, status);
    }
}

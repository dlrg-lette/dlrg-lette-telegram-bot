package org.dlrg.lette.telegrambot.data;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "adminChat")
public class AdminChat extends Chat {

    public int category;

    public AdminChat() {
        super();
    }

    public AdminChat(Long id, String status) {
        super(id, status);
    }
}

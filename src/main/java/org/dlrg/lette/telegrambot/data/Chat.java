package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat")
public class Chat {

    @Id
    public Long id;
    public String status;
    public String message;

    public Chat() {
    }

    public Chat(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    public Chat(Long id, String status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Chat " + id + " - " + status + " - " + message;
    }
}

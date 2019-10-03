package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Service;

@Service
public class Chat {

    @Id
    public String id;
    public String status;
    public String message;

    public Chat() {
    }

    public Chat(String status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Chat " + id + " - " + status + " - " + message;
    }
}

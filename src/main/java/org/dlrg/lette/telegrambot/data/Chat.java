package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;

public class Chat {

    @Id
    public Long id;
    public String status;
    public Integer lastMessage;

    public Chat() {
    }

    public Chat(Long id) {
        this.id = id;
    }

    public Chat(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Chat %d - %s", id, status);
    }
}

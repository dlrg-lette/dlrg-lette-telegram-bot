package org.dlrg.lette.telegrambot.data;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedList;
import java.util.List;

@Document(collection = "adminChat")
public class AdminChat extends Chat {

    private List<Integer> categories;

    public AdminChat() {
        super();
    }

    public AdminChat(Long id) {
        super(id);
    }

    public AdminChat(Long id, String status) {
        super(id, status);
        this.categories = new LinkedList<>();
    }

    public List<Integer> getCategories() {
        if (categories == null) {
            categories = new LinkedList<>();
        }
        return categories;
    }
}

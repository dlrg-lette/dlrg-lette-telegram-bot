package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;

import java.util.List;

public class User {

    @Id
    public Integer id;
    public List<Category> categories;

    public User() {
    }

    public User(Integer id) {
        this.id = id;
    }
}

package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "user")
public class User {

    @Id
    public Integer id;
    public List<Integer> categories;
    public String name;

    public User() {
    }

    public User(Integer id) {
        this.id = id;
        this.categories = new ArrayList<>();
    }

    public User(Integer id, String name) {
        this.id = id;
        this.name = name;
        this.categories = new ArrayList<>();
    }
}

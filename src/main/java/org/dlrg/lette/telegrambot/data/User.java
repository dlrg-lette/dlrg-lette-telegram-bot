package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "user")
public class User {

    @Id
    public Integer id;
    public List<Integer> categories;

    public User() {
    }

    public User(Integer id) {
        this.id = id;
    }
}

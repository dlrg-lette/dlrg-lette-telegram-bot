package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;

public class Moderator {

    @Id
    public Integer id;
    public String firstName;
    public boolean isAdministrator;

    public Moderator() {
    }

    public Moderator(Integer id, String firstName) {
        this.id = id;
        this.firstName = firstName;
    }
}

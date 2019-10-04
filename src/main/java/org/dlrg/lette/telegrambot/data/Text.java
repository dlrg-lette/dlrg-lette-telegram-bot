package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "texts")
public class Text {

    @Id
    public String id;
    public String text;

    public Text() {
    }
}

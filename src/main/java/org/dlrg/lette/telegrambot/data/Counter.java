package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "counter")
public class Counter {

    @Id
    public String id;
    public int sequenceValue;

    public Counter() {
    }
}

package org.dlrg.lette.telegrambot.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "category")
public class Category {

    @Id
    public Integer id;
    public String description;

    public Category() {
    }

    @Override
    public boolean equals(Object object) {
        if (object.getClass() == Category.class) {
            return this.id.equals(((Category) object).id);
        } else {
            return false;
        }
    }
}

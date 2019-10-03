package org.dlrg.lette.telegrambot.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface ChatRepository extends MongoRepository<Chat, Long> {
}

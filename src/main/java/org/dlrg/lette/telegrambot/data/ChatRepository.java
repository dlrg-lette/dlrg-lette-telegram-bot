package org.dlrg.lette.telegrambot.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

\|
@Repository y
public interface ChatRepository extends MongoRepository<Chat, Long> {

    public List<Chat> findByStatus(String status);
}

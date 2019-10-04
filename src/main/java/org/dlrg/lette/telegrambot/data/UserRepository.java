package org.dlrg.lette.telegrambot.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRepository extends MongoRepository<User, Integer> {

    @Query("{categories: ?0}")
    List<User> findAllByCategory(Integer category);
}

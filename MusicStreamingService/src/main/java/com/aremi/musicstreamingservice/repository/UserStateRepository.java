package com.aremi.musicstreamingservice.repository;

import com.aremi.musicstreamingservice.model.UserState;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStateRepository extends ReactiveMongoRepository<UserState, String> {
}

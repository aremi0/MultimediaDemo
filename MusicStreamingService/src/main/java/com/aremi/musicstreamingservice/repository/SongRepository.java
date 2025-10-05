package com.aremi.musicstreamingservice.repository;

import com.aremi.musicstreamingservice.model.Song;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SongRepository extends ReactiveMongoRepository<Song, String> {
    Flux<Song> findByTitleContainingIgnoreCase(String title);
    Flux<Song> findTopByOrderByPlayCountDesc();
}

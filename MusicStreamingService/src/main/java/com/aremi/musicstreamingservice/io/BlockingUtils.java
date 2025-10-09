package com.aremi.musicstreamingservice.io;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

public class BlockingUtils {

    public static <T> Mono<T> runBlocking(Supplier<T> blockingCall) {
        return Mono.fromCallable(blockingCall::get)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public static <T> Mono<T> runBlocking(String label, Supplier<T> blockingCall) {
        return Mono.fromCallable(() -> {
            System.out.println("⏳ runBlocking: " + label);
            return blockingCall.get();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}

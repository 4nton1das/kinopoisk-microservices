package com.example.kinopoiskservice.components;

import com.kinopoisk.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class GrpcTestClient implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GrpcTestClient.class);

    @GrpcClient("user-service")
    private UserRatingServiceGrpc.UserRatingServiceBlockingStub ratingStub;

    @Override
    public void run(String... args) {
        log.info("=== Проверка gRPC подключения ===");

        try {
            // Только проверка соединения (read-only операция)
            GetAverageRatingRequest request = GetAverageRatingRequest.newBuilder()
                    .setContentId(1L)
                    .build();

            AverageRatingResponse response = ratingStub.getAverageRating(request);
            log.info("gRPC соединение установлено. User-service отвечает.");

        } catch (Exception e) {
            log.warn("gRPC соединение не установлено: {}", e.getMessage());
            log.info("Запустите user-service на порту 9090 для полной функциональности");
        }
    }
}

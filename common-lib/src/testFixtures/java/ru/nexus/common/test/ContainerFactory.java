package ru.nexus.common.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

public class ContainerFactory {

    private static final String MONGO_VER = "mongo:7.0";
    private static final String POSTGRES_VER = "postgres:15-alpine";
    private static final String RABBIT_VER = "rabbitmq:3.12-management";
    private static final String REDIS_VER = "redis:7.2-alpine";

    public static MongoDBContainer mongo() {
        return new MongoDBContainer(MONGO_VER);
    }

    public static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(POSTGRES_VER);
    }

    public static RabbitMQContainer rabbit() {
        return new RabbitMQContainer(DockerImageName.parse(RABBIT_VER));
    }

    public static GenericContainer<?> redis() {
        return new GenericContainer<>(DockerImageName.parse(REDIS_VER))
                .withExposedPorts(6379);
    }
}
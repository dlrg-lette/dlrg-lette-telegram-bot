# 1st Docker build stage: build the project with maven
FROM maven:3.6.3-openjdk-11 as builder
WORKDIR /project
COPY . /project/
RUN mvn package -DskipTests -B

# 2nd Docker build stage: copy builder output and configure entry point
FROM eclipse-temurin:11-jre

LABEL author="Simon Ameling / Ayokas"
LABEL version="1.0.2"

# Config via environment variables
ENV APP_DIR /telegram-bot-config
ENV APP_FILE telegram-bot.jar

# Spring configuration
ENV SPRING_PROFILE_ACTIVE=application
ENV SPRING_CONFIG_LOCATION=${APP_DIR}

# Application configuration
ENV ADMIN_BOT_TOKEN ""
ENV SENDER_BOT_TOKEN ""
ENV EXTERNAL_ADMIN_ADDRESS ""
ENV EXTERNAL_SENDER_URL ${EXTERNAL_ADMIN_ADDRESS}
ENV EXTERNAL_PORT 443
ENV CONTAINER_PORT 8080
ENV BOT_NAME ""

# MongoDB Connection Params
ENV MONGO_HOST "mongodb"
ENV MONGO_PORT "27017"
ENV MONGO_DB ""
ENV MONGO_USER ""
ENV MONGO_PW ""
ENV MONGO_AUTH_DB "admin"

# Volume for configuration
VOLUME ${SPRING_CONFIG_LOCATION}

WORKDIR ${APP_DIR}
COPY --from=builder /project/target/*.jar /var/lib/telegramBot/${APP_FILE}
COPY --from=builder /project/docker-example.properties ${APP_DIR}/${SPRING_PROFILE_ACTIVE}.properties

ENTRYPOINT [ "sh", "-c" ]
CMD ["exec java -jar /var/lib/telegramBot/${APP_FILE} --spring.config.location=${SPRING_CONFIG_LOCATION}/${SPRING_PROFILE_ACTIVE}.properties --spring-boot.run.profiles=${SPRING_PROFILE_ACTIVE}"]

# Healthcheck
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 CMD [ "(curl -o /dev/null -s -w '%{http_code}\n' http://localhost:${CONTAINER_PORT})" == "200" ]
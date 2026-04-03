FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy

RUN groupadd --system appgroup && useradd --system --gid appgroup --no-create-home appuser

WORKDIR /app

ARG JAR_FILE=build/libs/User-Service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} application.jar

EXPOSE 8080

RUN chown -R appuser:appgroup /app
USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar application.jar"]

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY parse-workers/java-parser/pom.xml parse-workers/java-parser/

RUN ./mvnw -pl parse-workers/java-parser dependency:go-offline -B

COPY shared-contracts/proto/ shared-contracts/proto/
COPY parse-workers/java-parser/ parse-workers/java-parser/
RUN ./mvnw -pl parse-workers/java-parser clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/parse-workers/java-parser/target/*.jar app.jar

USER appuser

EXPOSE 8083 9093

ENTRYPOINT ["java", "-Dspring.threads.virtual.enabled=true", "-jar", "app.jar"]

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY data-plane/pom.xml data-plane/
COPY data-plane/data-common/pom.xml data-plane/data-common/
COPY data-plane/data-domain/pom.xml data-plane/data-domain/
COPY data-plane/data-api/pom.xml data-plane/data-api/
COPY data-plane/data-infrastructure/pom.xml data-plane/data-infrastructure/
COPY data-plane/data-application/pom.xml data-plane/data-application/
COPY data-plane/data-boot/pom.xml data-plane/data-boot/

RUN ./mvnw -pl data-plane -am dependency:go-offline -B

COPY data-plane/ data-plane/
RUN ./mvnw -pl data-plane -am clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/data-plane/data-boot/target/*.jar app.jar

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-Dspring.threads.virtual.enabled=true", "-jar", "app.jar"]

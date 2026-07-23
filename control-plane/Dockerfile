# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY control-plane/pom.xml control-plane/
COPY control-plane/control-common/pom.xml control-plane/control-common/
COPY control-plane/control-domain/pom.xml control-plane/control-domain/
COPY control-plane/control-api/pom.xml control-plane/control-api/
COPY control-plane/control-infrastructure/pom.xml control-plane/control-infrastructure/
COPY control-plane/control-application/pom.xml control-plane/control-application/
COPY control-plane/control-boot/pom.xml control-plane/control-boot/

RUN ./mvnw -pl control-plane -am dependency:go-offline -B

COPY control-plane/ control-plane/
RUN ./mvnw -pl control-plane -am clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/control-plane/control-boot/target/*.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-Dspring.threads.virtual.enabled=true", "-jar", "app.jar"]

FROM maven:3.8.4-openjdk-11-slim AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM adoptopenjdk/openjdk11:latest
WORKDIR /
ENV INSTANCE_RANDOM true
ENV EPSILON 0.001
ENV PRINT_RESULT false
ENV INSTANCE_MINJOBS 20
ENV INSTNACE_MAXJOBS 200
ENV INSTANCE_MINMACHINES 50
ENV INSTANCE_MAXMACHINES 100
ENV INSTANCE_MAX_SEQUENTIAL_TIME 100
ENV ALGO Ohnesorge

COPY --from=builder /app/target/bachelorarbeit-1.0-SNAPSHOT-jar-with-dependencies.jar Bachelorarbeit.jar
ENTRYPOINT ["java", "-jar", "Bachelorarbeit.jar"]

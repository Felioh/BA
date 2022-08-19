FROM adoptopenjdk/openjdk11:latest
WORKDIR /
ENV INSTANCE_RANDOM true
ENV INSTANCE_MINJOBS 100
ENV INSTNACE_MAXJOBS 1000
ENV INSTANCE_MINMACHINES 50
ENV INSTANCE_MAXMACHINES 200
ENV ELASTICSEARCH_HOST "172.18.0.2"
ENV LOOP true
ENV ALGO Kilian

ADD Bachelorarbeit.jar Bachelorarbeit.jar
ENTRYPOINT ["java", "-jar", "Bachelorarbeit.jar"]

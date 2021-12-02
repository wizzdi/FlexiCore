FROM alpine/git as clone
WORKDIR /app
RUN git clone https://github.com/wizzdi/FlexiCore.git

FROM maven:3.6.3-openjdk-11 as build
WORKDIR /app
COPY --from=clone /app/FlexiCore /app
RUN mvn install -DskipTests

FROM adoptopenjdk/openjdk11 as run
WORKDIR /app
COPY --from=build /app/target/FlexiCore-*-SNAPSHOT-exec.jar /app/FlexiCore.jar

EXPOSE 8080
EXPOSE 8787

CMD ["/bin/bash","-c","java -jar /app/FlexiCore.jar"]


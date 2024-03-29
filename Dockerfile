# syntax=docker/dockerfile:1

FROM maven:3.8.6-eclipse-temurin-17 as build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn package -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -DskipTests

FROM eclipse-temurin:17-alpine as run
WORKDIR /app
COPY --from=build /app/target/FlexiCore-*-SNAPSHOT-exec.jar /app/FlexiCore.jar

EXPOSE 8080
EXPOSE 8787
VOLUME /home/flexicore/upload
VOLUME /home/flexicore/users
VOLUME /home/flexicore/plugins
VOLUME /home/flexicore/entities


CMD java $JAVA_OPTS  -Dloader.main=com.wizzdi.flexicore.init.FlexiCoreApplication -Dloader.path=file:/home/flexicore/entities/ -Dloader.debug=true -cp /app/FlexiCore.jar org.springframework.boot.loader.PropertiesLauncher


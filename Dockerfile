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

RUN apt-get update && apt-get -qq -y install wget libcurl4 openssl liblzma5  gnupg maven lsb-release
RUN mkdir -p /var/lib/mongodb &&  mkdir -p /var/log/mongodb
RUN cd /var/lib/mongodb&&wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-ubuntu1804-4.4.1.tgz&&tar -zxvf mongodb-*-4.4.1.tgz&&cd mongodb* &&cp bin/* /usr/bin/

ENV TZ=Israel
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get clean &&apt-get update && apt-get -qq -y install postgresql postgresql-contrib vim net-tools psmisc
# Run the rest of the commands as the ``postgres`` user created by the ``postgres-9.3`` package when it was ``apt-get installed``

USER postgres

# Create a PostgreSQL role named ``docker`` with ``docker`` as the password and
# then create a database `docker` owned by the ``docker`` role.
# Note: here we use ``&&\`` to run commands one after the other - the ``\``
#       allows the RUN command to span multiple lines.
RUN    /etc/init.d/postgresql start &&\
    psql --command "CREATE USER flexicore WITH SUPERUSER PASSWORD 'flexicore';" &&\
    createdb -O flexicore flexicore

# Adjust PostgreSQL configuration so that remote connections to the
# daitabase are possible.
RUN cd /etc/postgresql/*/main/&&echo "host all  all    0.0.0.0/0  md5" >>pg_hba.conf && echo "listen_addresses='*'" >>postgresql.conf

# Expose the PostgreSQL port
EXPOSE 5432

# Add VOLUMEs to allow backup of config, logs and databases
VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"]

EXPOSE 8080
EXPOSE 8787
USER root
RUN mkdir -p /data/db
CMD ["/bin/bash","-c","/usr/sbin/service postgresql start&&/usr/bin/mongod --fork --logpath /var/log/mongodb/mongod.log --dbpath /var/lib/mongodb&&java -jar /app/FlexiCore.jar"]


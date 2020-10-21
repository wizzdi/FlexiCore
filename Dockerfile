FROM alpine/git as clone
WORKDIR /app
RUN git clone https://github.com/wizzdi/FlexiCore.git

FROM maven:3.6.3-openjdk-11 as build
WORKDIR /app
COPY --from=clone /app/FlexiCore /app
RUN mvn install -DskipTests

FROM adoptopenjdk/openjdk11
WORKDIR /app
COPY --from=build /app/target/FlexiCore-4.2.2-SNAPSHOT-exec.jar /app


RUN apt-get update && apt-get -qq -y install wget gnupg maven python2.7 python-pip

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2930ADAE8CAF5059EE73BB4B58712A2291FA4AD5
RUN echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/3.6 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-3.6.list

ENV TZ=Israel
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get clean &&apt-get update && apt-get -qq -y install software-properties-common postgresql-10 postgresql-client-10 postgresql-contrib-10 vim net-tools psmisc mongodb-org
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
# database are possible.
RUN echo "host all  all    0.0.0.0/0  md5" >> /etc/postgresql/10/main/pg_hba.conf

# And add ``listen_addresses`` to ``/etc/postgresql/9.3/main/postgresql.conf``
RUN echo "listen_addresses='*'" >> /etc/postgresql/10/main/postgresql.conf

# Expose the PostgreSQL port
EXPOSE 5432

# Add VOLUMEs to allow backup of config, logs and databases
VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"]

EXPOSE 8080
EXPOSE 8787
USER root
RUN mkdir -p /data/db
CMD ["/bin/bash","-c","/usr/sbin/service postgresql start&&/usr/bin/mongod --fork --logpath /var/log/mongodb/mongod.log&&java -jar /app/FlexiCore-4.2.2-SNAPSHOT-exec.jar"]

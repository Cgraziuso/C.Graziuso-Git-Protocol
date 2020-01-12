FROM maven:3 as builder
RUN apt-get update && apt-get -y install git
ARG url
WORKDIR /app
RUN git clone ${url}
ARG project
WORKDIR /app/${project} 
RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /app
ARG project
ARG artifactid
ARG version
ENV artifact ${artifactid}-${version}.jar
ENV MASTERIP=127.0.0.1
ENV ID=0
COPY --from=builder /app/${project}/target/${artifact} /app

CMD /usr/bin/java -jar ${artifact} -m $MASTERIP -id $ID

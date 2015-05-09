FROM pires/docker-jre
MAINTAINER Paulo Pires <pjpires@gmail.com>

EXPOSE 5701

RUN \
  curl -Lskj https://github.com/pires/hazelcast-kubernetes-bootstrapper/releases/download/0.2/hazelcast-kubernetes-bootstrapper-0.2-SNAPSHOT.jar \
  -o /bootstrapper.jar

CMD java -jar /bootstrapper.jar

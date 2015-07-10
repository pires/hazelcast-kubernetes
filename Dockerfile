FROM quay.io/pires/docker-jre:8u45-3

MAINTAINER Paulo Pires <pjpires@gmail.com>

EXPOSE 5701

RUN \
  curl -Lskj https://github.com/pires/hazelcast-kubernetes-bootstrapper/releases/download/0.4/hazelcast-kubernetes-bootstrapper-0.4.jar \
  -o /bootstrapper.jar

CMD java -jar /bootstrapper.jar

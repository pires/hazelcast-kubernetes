FROM quay.io/pires/docker-jre:8u92-r1

MAINTAINER Paulo Pires <pjpires@gmail.com>

EXPOSE 5701

RUN \
  apk add --update curl ca-certificates; apk upgrade; \
  curl -Lskj https://github.com/pires/hazelcast-kubernetes-bootstrapper/releases/download/0.6.1/hazelcast-kubernetes-bootstrapper-0.6.1.jar \
  -o /bootstrapper.jar;\
  apk del curl wget; \
  rm /var/cache/apk/*

CMD java -jar /bootstrapper.jar

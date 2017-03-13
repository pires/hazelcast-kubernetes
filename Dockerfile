FROM quay.io/pires/docker-jre:8u112_1

label maintainer Paulo Pires <pjpires@gmail.com>

env VERSION 3.8

RUN \
  apk add --update curl ca-certificates; apk upgrade; \
  curl -Lskj https://github.com/pires/hazelcast-kubernetes-bootstrapper/releases/download/$VERSION/hazelcast-kubernetes-bootstrapper-$VERSION.jar \
  -o /bootstrapper.jar;\
  apk del curl wget; \
  rm /var/cache/apk/*

CMD java -jar /bootstrapper.jar

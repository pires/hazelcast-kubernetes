# You know you love it
FROM ubuntu:14.04

# Me, Myself and I
MAINTAINER Paulo Pires <pjpires@gmail.com>

RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list
RUN apt-get update
RUN apt-get upgrade -y

# Install Oracle JRE 8
RUN apt-get -y install software-properties-common
RUN  add-apt-repository ppa:webupd8team/java
RUN apt-get -y update
RUN echo "oracle-java8-installer  shared/accepted-oracle-license-v1-1 boolean true" | debconf-set-selections
RUN apt-get -y install oracle-java8-installer wget
RUN apt-get install oracle-java8-set-default

# Download hazelcast-kubernetes-bootstrapper & run
RUN mkdir /opt/hazelcast-k8s
ADD hazelcast-kubernetes-bootstrapper/target/hazelcast-kubernetes-0.1-SNAPSHOT.jar /opt/hazelcast-k8s/bootstrapper.jar
CMD java -jar /opt/hazelcast-k8s/bootstrapper.jar

EXPOSE 5701

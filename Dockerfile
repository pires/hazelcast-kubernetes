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
RUN apt-get -y install oracle-java8-installer maven git
RUN apt-get install oracle-java8-set-default

# Build hazelcast-kubernetes-bootstrapper
RUN mkdir /opt/hazelcast-k8s
WORKDIR /opt/hazelcast-k8s
RUN git clone https://github.com/pires/hazelcast-kubernetes.git
WORKDIR /opt/hazelcast-k8s/hazelcast-kubernetes/hazelcast-kubernetes-bootstrapper
RUN mvn clean package
ADD target/hazelcast-kubernetes-0.1-SNAPSHOT.jar /opt/hazelcast-k8s/bootstrapper.jar
WORKDIR /opt/hazelcast-k8s

# clean-up
RUN rm -rf hazelcast-kubernetes
RUN apt-get remove --purge maven git
RUN apt-get autoremove
RUN apt-get autoclean

# run
CMD java -jar bootstrapper.jar

EXPOSE 5701

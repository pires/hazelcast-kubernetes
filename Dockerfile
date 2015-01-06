# You know you love it
FROM ubuntu:14.04

# Me, Myself and I
MAINTAINER Paulo Pires <pjpires@gmail.com>

RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list && \
    apt-get update && \
    apt-get upgrade -y

# Install Oracle JRE 8
RUN apt-get -y install software-properties-common && \
    add-apt-repository ppa:webupd8team/java && \
    apt-get -y update && \
    echo "oracle-java8-installer  shared/accepted-oracle-license-v1-1 boolean true" | debconf-set-selections && \
    apt-get -y install oracle-java8-installer maven git && \
    apt-get install oracle-java8-set-default && \
    apt-get autoclean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /var/cache/oracle-jdk8-installer

# Build hazelcast-kubernetes-bootstrapper
RUN mkdir /opt/hazelcast-k8s
WORKDIR /opt/hazelcast-k8s
RUN git clone https://github.com/pires/hazelcast-kubernetes-bootstrapper.git
WORKDIR /opt/hazelcast-k8s/hazelcast-kubernetes-bootstrapper
ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8
RUN mvn clean package && \
    mv target/hazelcast-kubernetes-0.1-SNAPSHOT.jar /opt/hazelcast-k8s/bootstrapper.jar && \
    cd .. && \
    rm -rf hazelcast-kubernetes-bootstrapper
WORKDIR /opt/hazelcast-k8s

# run
CMD java -jar bootstrapper.jar

EXPOSE 5701

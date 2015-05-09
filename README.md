hazelcast-kubernetes
====================

Hazelcast clustering for Kubernetes made easy.

Lean (186MB) JRE 8 + Hazelcast 3.4.2 + Kubernetes Hazelcast discovery Docker image, based on progrium/busybox.

## Pre-requisites

* Docker 1.5+
* Kubernetes 0.16.2+ cluster

## Kubernetes cluster

You can test with a local cluster. Check [this other repository from yours truly](https://github.com/pires/kubernetes-vagrant-coreos-cluster).

## Docker image

The image is already available at [Docker Hub](https://registry.hub.docker.com/u/pires/hazelcast-k8s/)

## Deploy

### Service

So that you can access the cluster, you need to deploy a ```service```.

```
kubectl create -f hazelcast-service.yaml
```

Confirm service has been created:

```
kubecfg list services
```

### Replication Controller

Let's start with a 1-replica controller:

```
kubectl create -f hazelcast-controller.yaml
```

Check the ```pods``` list:

```
kubectl list pods
```

You should see **one** Hazelcast pod.

Let's scale it up:

```
kubectl resize --replicas=2 replicationcontroller hazelcast
```

Check the ```pods``` list once more.

After all pods are running, and by inspecting their logs, you should confirm that Hazelcast nodes are connected to each other.

```
kubectl log <pod identifier> hazelcast
```

You should see something like:

```
2015-05-09 22:06:20.016  INFO 5 --- [           main] com.github.pires.hazelcast.Application   : Starting Application v0.2-SNAPSHOT on hazelcast-enyli with PID 5 (/bootstrapper.jar started by root in /)
2015-05-09 22:06:20.071  INFO 5 --- [           main] s.c.a.AnnotationConfigApplicationContext : Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@5424f110: startup date [Sat May 09 22:06:20 GMT 2015]; root of context hierarchy
2015-05-09 22:06:21.511  INFO 5 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
2015-05-09 22:06:21.549  INFO 5 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Asking k8s registry at http://10.100.0.1:80..
2015-05-09 22:06:22.031  INFO 5 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Found 2 pods running Hazelcast.
2015-05-09 22:06:22.176  INFO 5 --- [           main] c.h.instance.DefaultAddressPicker        : [LOCAL] [someGroup] [3.4.2] Interfaces is disabled, trying to pick one address from TCP-IP config addresses: [10.244.90.3, 10.244.66.2]
2015-05-09 22:06:22.177  INFO 5 --- [           main] c.h.instance.DefaultAddressPicker        : [LOCAL] [someGroup] [3.4.2] Prefer IPv4 stack is true.
2015-05-09 22:06:22.189  INFO 5 --- [           main] c.h.instance.DefaultAddressPicker        : [LOCAL] [someGroup] [3.4.2] Picked Address[10.244.66.2]:5701, using socket ServerSocket[addr=/0:0:0:0:0:0:0:0,localport=5701], bind any local is true
2015-05-09 22:06:22.642  INFO 5 --- [           main] com.hazelcast.spi.OperationService       : [10.244.66.2]:5701 [someGroup] [3.4.2] Backpressure is disabled
2015-05-09 22:06:22.647  INFO 5 --- [           main] c.h.spi.impl.BasicOperationScheduler     : [10.244.66.2]:5701 [someGroup] [3.4.2] Starting with 2 generic operation threads and 2 partition operation threads.
2015-05-09 22:06:22.796  INFO 5 --- [           main] com.hazelcast.system                     : [10.244.66.2]:5701 [someGroup] [3.4.2] Hazelcast 3.4.2 (20150326 - f6349a4) starting at Address[10.244.66.2]:5701
2015-05-09 22:06:22.798  INFO 5 --- [           main] com.hazelcast.system                     : [10.244.66.2]:5701 [someGroup] [3.4.2] Copyright (C) 2008-2014 Hazelcast.com
2015-05-09 22:06:22.800  INFO 5 --- [           main] com.hazelcast.instance.Node              : [10.244.66.2]:5701 [someGroup] [3.4.2] Creating TcpIpJoiner
2015-05-09 22:06:22.801  INFO 5 --- [           main] com.hazelcast.core.LifecycleService      : [10.244.66.2]:5701 [someGroup] [3.4.2] Address[10.244.66.2]:5701 is STARTING
2015-05-09 22:06:23.108  INFO 5 --- [cached.thread-2] com.hazelcast.nio.tcp.SocketConnector    : [10.244.66.2]:5701 [someGroup] [3.4.2] Connecting to /10.244.90.3:5701, timeout: 0, bind-any: true
2015-05-09 22:06:23.182  INFO 5 --- [cached.thread-2] c.h.nio.tcp.TcpIpConnectionManager       : [10.244.66.2]:5701 [someGroup] [3.4.2] Established socket connection between /10.244.66.2:48051 and 10.244.90.3/10.244.90.3:5701
2015-05-09 22:06:29.158  INFO 5 --- [ration.thread-1] com.hazelcast.cluster.ClusterService     : [10.244.66.2]:5701 [someGroup] [3.4.2]

Members [2] {
	Member [10.244.90.3]:5701
	Member [10.244.66.2]:5701 this
}

2015-05-09 22:06:31.177  INFO 5 --- [           main] com.hazelcast.core.LifecycleService      : [10.244.66.2]:5701 [someGroup] [3.4.2] Address[10.244.66.2]:5701 is STARTED
2015-05-09 22:06:31.179  INFO 5 --- [           main] com.github.pires.hazelcast.Application   : Started Application in 11.707 seconds (JVM running for 12.542)
```

## Accessing cluster

The service should now be available at ```$HAZELCAST_SERVICE_HOST```:```$HAZELCAST_SERVICE_PORT```. Of course, this is only accessible from inside the cluster (say, another pod).

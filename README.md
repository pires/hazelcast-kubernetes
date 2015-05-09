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
kubectl resize --replicas=3 replicationcontroller hazelcast
```

Check the ```pods``` list once more.

After all pods are running, and by inspecting their logs, you should confirm that Hazelcast nodes are connected to each other.

```
kubectl log <pod identifier> hazelcast
```

You should see something like:

```
2014-12-24T01:21:09.731468790Z 2014-12-24 01:21:09.701  INFO 10 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Asking k8s registry at http://10.160.211.80:80..
2014-12-24T01:21:13.686978543Z 2014-12-24 01:21:13.686  INFO 10 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Found 3 pods running Hazelcast.
2014-12-24T01:21:13.772599736Z 2014-12-24 01:21:13.772  INFO 10 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Added member 10.160.2.3
2014-12-24T01:21:13.783689690Z 2014-12-24 01:21:13.783  INFO 10 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Added member 10.160.2.4
2014-12-24T01:21:13.783947139Z 2014-12-24 01:21:13.783  INFO 10 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Added member 10.160.1.3

(...)

2014-12-24T01:21:16.001954855Z 2014-12-24 01:21:15.999  INFO 10 --- [cached.thread-2] c.h.nio.tcp.TcpIpConnectionManager       : [10.160.2.4]:5701 [someGroup] [3.3.3] Established socket connection between /10.160.2.4:59686 and /10.160.1.3:5701
2014-12-24T01:21:16.007729519Z 2014-12-24 01:21:16.000  INFO 10 --- [cached.thread-3] c.h.nio.tcp.TcpIpConnectionManager       : [10.160.2.4]:5701 [someGroup] [3.3.3] Established socket connection between /10.160.2.4:54931 and /10.160.2.3:5701
2014-12-24T01:21:16.427289059Z 2014-12-24 01:21:16.427  INFO 10 --- [thread-Acceptor] com.hazelcast.nio.tcp.SocketAcceptor     : [10.160.2.4]:5701 [someGroup] [3.3.3] Accepting socket connection from /10.160.2.3:50660
2014-12-24T01:21:16.433763738Z 2014-12-24 01:21:16.433  INFO 10 --- [cached.thread-3] c.h.nio.tcp.TcpIpConnectionManager       : [10.160.2.4]:5701 [someGroup] [3.3.3] Established socket connection between /10.160.2.4:5701 and /10.160.2.3:50660
2014-12-24T01:21:23.036227250Z 2014-12-24 01:21:23.035  INFO 10 --- [ration.thread-1] com.hazelcast.cluster.ClusterService     : [10.160.2.4]:5701 [someGroup] [3.3.3]
2014-12-24T01:21:23.036227250Z
2014-12-24T01:21:23.036227250Z Members [3] {
2014-12-24T01:21:23.036227250Z 	Member [10.160.1.3]:5701
2014-12-24T01:21:23.036227250Z 	Member [10.160.2.4]:5701 this
2014-12-24T01:21:23.036227250Z 	Member [10.160.2.3]:5701
2014-12-24T01:21:23.036227250Z }
```

## Accessing cluster

The service should now be available at ```$HAZELCAST_SERVICE_HOST```:```$HAZELCAST_SERVICE_PORT```. Of course, this is only accessible from inside the cluster (say, another pod).

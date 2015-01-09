hazelcast-kubernetes
====================

Hazelcast clustering for Kubernetes made easy.

## Pre-requisites

* Docker 1.3+
* Kubernetes cluster

## Kubernetes cluster

You can use GCE or a local cluster. For a local cluster on MacOS X or Linux, just hit [this other repository from yours truly](https://github.com/pires/kubernetes-vagrant-coreos-cluster).

## Build Docker image

The image is already available at [Docker Hub](https://registry.hub.docker.com/u/pires/hazelcast-k8s/). You can:

```
docker pull pires/hazelcast-k8s
```

But if you feel like building it yourself:

```
docker build -t hazelcast-k8s:0.1 .
```

## Deploy

### Service 

So that you can access the cluster, you need to deploy a ```service```.

**GCE**
```
gcloud preview container services create --config-file=hazelcast-k8s-service.json
```

**Local cluster**
```
kubectl create -f hazelcast-k8s-service.json
```

Confirm service has been created:

**GCE**
```
gcloud preview container services list
```

**Local cluster**
```
kubecfg list services
```


### Replication Controller

Let's start with a 1-replica controller:

**GCE**

```
gcloud preview container replicationcontrollers create --config-file=hazelcast-k8s-controller.json
```

**Local cluster**
```
kubectl create -f hazelcast-k8s-controller.json
```

Check the ```pods``` list:

**GCE**
```
gcloud preview container pods list
```

**Local cluster**
```
kubecfg list pods
```

You should see one replica of an Hazelcast node.

Let's now scale it up:

**GCE**
```
gcloud preview container replicationcontrollers resize hazelcast --num-replicas=3
```

**Local cluster**
```
kubecfg resize hazelcast 3
```

Check the ```pods``` list once more.

After all pods are running, and by inspecting their logs, you should confirm that Hazelcast nodes are connected to each other.

**GCE**
```
gcloud preview container kubectl log <pod identifier> hazelcast
```

**Local cluster**
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

hazelcast-kubernetes
====================

Hazelcast clustering for Kubernetes made easy.

## Pre-requisites

* Docker 1.3+
* Kubernetes binaries or ```gcloud``` (GCE)

## Build

The image is already available at [Docker Hub](https://registry.hub.docker.com/u/pires/hazelcast-k8s/). You can:

```
docker pull pires/hazelcast-k8s
```

But if you feel like building it yourself:

```
docker build -t hazelcast-k8s:0.1 .
```

## Deploy

### Replication Controller

Let's start with a 1-replica controller:

```
gcloud preview container replicationcontrollers create --config-file=hazelcast-k8s-controller.json
```

Check the ```pods``` list:

```
gcloud preview container pods list
```

You should see one replica of an Hazelcast node.

Let's now scale it up:

```
gcloud preview container replicationcontrollers resize hazelcast-k8s-controller --num-replicas=3
```

Check the ```pods``` list once more.

After all pods are running, and by inspecting their logs, you should confirm that Hazelcast nodes are connected to each other.

### Service 

So that you can access the cluster, you need to deploy a ```service```.

```
gcloud preview container services create --config-file=hazelcast-k8s-service.json
```

Confirm service has been create:

```
gcloud preview container services list
```

## Accessing cluster

The service should now be available at ```$HAZELCAST_SERVICE_HOST```:```$HAZELCAST_SERVICE_PORT". Of course, this is only accessible from inside the cluster (say, another pod).

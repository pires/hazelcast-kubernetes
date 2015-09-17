hazelcast-kubernetes
====================

Hazelcast clustering for Kubernetes made easy.

Lean (185MB) Hazelcast with  Kubernetes discovery support container image, based on `alpine`.

## Software

* JRE 8u60
* Hazelcast 3.5.2

## Pre-requisites

* Docker 1.7.1+
* Kubernetes 1.0 or newer cluster

## Kubernetes cluster

You can test with a local cluster. Check [this other repository from yours truly](https://github.com/pires/kubernetes-vagrant-coreos-cluster).

## Docker image

The image is already available at [quay.io/pires](https://quay.io/repository/pires/hazelcast-kubernetes)

## Cloud Native Deployments of Hazelcast using Kubernetes

The following document describes the development of a _cloud native_ [Hazelcast](http://hazelcast.org/) deployment on Kubernetes.  When we say _cloud native_ we mean an application which understands that it is running within a cluster manager, and uses this cluster management infrastructure to help implement the application. In particular, in this instance, a custom Hazelcast ```bootstrapper``` is used to enable Hazelcast to dynamically discover Hazelcast nodes that have already joined the cluster.

Any topology changes are communicated and handled by Hazelcast nodes themselves.

This document also attempts to describe the core components of Kubernetes, _Pods_, _Services_ and _Replication Controllers_.

### Prerequisites
This example assumes that you have a Kubernetes cluster installed and running, and that you have installed the `kubectl` command line tool somewhere in your path.  Please see the [getting started](https://github.com/GoogleCloudPlatform/kubernetes/tree/master/docs/getting-started-guides) for installation instructions for your platform.

### A note for the impatient
This is a somewhat long tutorial.  If you want to jump straight to the "do it now" commands, please see the [tl; dr](#tl-dr) at the end.

### Sources

Source is freely available at:
* Hazelcast Discovery - https://github.com/pires/hazelcast-kubernetes-bootstrapper
* Dockerfile - https://github.com/pires/hazelcast-kubernetes
* Docker Trusted Build - https://registry.hub.docker.com/u/pires/hazelcast-k8s

### Simple Single Pod Hazelcast Node
In Kubernetes, the atomic unit of an application is a [_Pod_](http://docs.k8s.io/pods.md).  A Pod is one or more containers that _must_ be scheduled onto the same host.  All containers in a pod share a network namespace, and may optionally share mounted volumes. 

In this case, we shall not run a single Hazelcast pod, because the discovery mechanism now relies on a service definition.


### Adding a Hazelcast Service
In Kubernetes a _Service_ describes a set of Pods that perform the same task.  For example, the set of nodes in a Hazelcast cluster.  An important use for a Service is to create a load balancer which distributes traffic across members of the set.  But a _Service_ can also be used as a standing query which makes a dynamically changing set of Pods available via the Kubernetes API.  This is actually how our discovery mechanism works, by relying on the service to discover other Hazelcast pods.

Here is the service description:
```yaml
apiVersion: v1
kind: Service
metadata: 
  labels: 
    name: hazelcast
  name: hazelcast
spec: 
  ports:
    - port: 5701
  selector: 
    name: hazelcast
```

The important thing to note here is the `selector`. It is a query over labels, that identifies the set of _Pods_ contained by the _Service_.  In this case the selector is `name: hazelcast`.  If you look at the Replication Controller specification below, you'll see that the pod has the corresponding label, so it will be selected for membership in this Service.

Create this service as follows:
```sh
$ kubectl create -f hazelcast-service.yaml
```

### Adding replicated nodes
The real power of Kubernetes and Hazelcast lies in easily building a replicated, resizable Hazelcast cluster.

In Kubernetes a _Replication Controller_ is responsible for replicating sets of identical pods.  Like a _Service_ it has a selector query which identifies the members of it's set.  Unlike a _Service_ it also has a desired number of replicas, and it will create or delete _Pods_ to ensure that the number of _Pods_ matches up with it's desired state.

Replication Controllers will "adopt" existing pods that match their selector query, so let's create a Replication Controller with a single replica to adopt our existing Hazelcast Pod.

```yaml
apiVersion: v1
kind: ReplicationController
metadata: 
  labels: 
    name: hazelcast
  name: hazelcast
spec: 
  replicas: 1
  selector: 
    name: hazelcast
  template: 
    metadata: 
      labels: 
        name: hazelcast
    spec: 
      containers: 
          image: quay.io/pires/hazelcast-kubernetes:0.5.1
          name: hazelcast
          env:
          - name: "DNS_DOMAIN"
            value: "cluster.local"
          ports: 
            - containerPort: 5701
              name: hazelcast
```

You may note that we tell Kubernetes that the container exposes the `hazelcast` port.  Finally, we tell the cluster manager that we need 1 cpu core.

The bulk of the replication controller config is actually identical to the Hazelcast pod declaration above, it simply gives the controller a recipe to use when creating new pods.  The other parts are the `selector` which contains the controller's selector query, and the `replicas` parameter which specifies the desired number of replicas, in this case 1.

Last but not least, we set `DNS_DOMAIN` environment variable according to your Kubernetes clusters DNS configuration.

Create this controller:

```sh
$ kubectl create -f hazelcast-controller.yaml
```

After the controller provisions successfully the pod, you can query the service endpoints:
```sh
$ kubectl get endpoints hazelcast -o yaml
{
    "kind": "Endpoints",
    "apiVersion": "v1",
    "metadata": {
        "name": "hazelcast",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/endpoints/hazelcast",
        "uid": "094e507a-2700-11e5-abbc-080027eae546",
        "resourceVersion": "4094",
        "creationTimestamp": "2015-07-10T12:34:41Z",
        "labels": {
            "name": "hazelcast"
        }
    },
    "subsets": [
        {
            "addresses": [
                {
                    "ip": "10.244.37.3",
                    "targetRef": {
                        "kind": "Pod",
                        "namespace": "default",
                        "name": "hazelcast-nsyzn",
                        "uid": "f57eb6b0-2706-11e5-abbc-080027eae546",
                        "resourceVersion": "4093"
                    }
                }
            ],
            "ports": [
                {
                    "port": 5701,
                    "protocol": "TCP"
                }
            ]
        }
    ]
}
```

You can see that the _Service_ has found the pod created by the replication controller.

Now it gets even more interesting.

Let's scale our cluster to 2 pods:
```sh
$ kubectl scale rc hazelcast --replicas=2
```

Now if you list the pods in your cluster, you should see two hazelcast pods:

```sh
$ kubectl get pods
NAME              READY     STATUS    RESTARTS   AGE
hazelcast-nanfb   1/1       Running   0          40s
hazelcast-nsyzn   1/1       Running   0          2m
kube-dns-xudrp    3/3       Running   0          1h
```

To prove that this all works, you can use the `log` command to examine the logs of one pod, for example:

```sh
$ kubectl log hazelcast-nanfb hazelcast
2015-07-10 13:26:34.443  INFO 5 --- [           main] com.github.pires.hazelcast.Application   : Starting Application on hazelcast-nanfb with PID 5 (/bootstrapper.jar started by root in /)
2015-07-10 13:26:34.535  INFO 5 --- [           main] s.c.a.AnnotationConfigApplicationContext : Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@42cfcf1: startup date [Fri Jul 10 13:26:34 GMT 2015]; root of context hierarchy
2015-07-10 13:26:35.888  INFO 5 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
2015-07-10 13:26:35.924  INFO 5 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Asking k8s registry at https://kubernetes.default.svc.cluster.local..
2015-07-10 13:26:37.259  INFO 5 --- [           main] c.g.p.h.HazelcastDiscoveryController     : Found 2 pods running Hazelcast.
2015-07-10 13:26:37.404  INFO 5 --- [           main] c.h.instance.DefaultAddressPicker        : [LOCAL] [someGroup] [3.5.2] Interfaces is disabled, trying to pick one address from TCP-IP config addresses: [10.244.77.3, 10.244.37.3]
2015-07-10 13:26:37.405  INFO 5 --- [           main] c.h.instance.DefaultAddressPicker        : [LOCAL] [someGroup] [3.5.2] Prefer IPv4 stack is true.
2015-07-10 13:26:37.415  INFO 5 --- [           main] c.h.instance.DefaultAddressPicker        : [LOCAL] [someGroup] [3.5.2] Picked Address[10.244.77.3]:5701, using socket ServerSocket[addr=/0:0:0:0:0:0:0:0,localport=5701], bind any local is true
2015-07-10 13:26:37.852  INFO 5 --- [           main] com.hazelcast.spi.OperationService       : [10.244.77.3]:5701 [someGroup] [3.5.2] Backpressure is disabled
2015-07-10 13:26:37.879  INFO 5 --- [           main] c.h.s.i.o.c.ClassicOperationExecutor     : [10.244.77.3]:5701 [someGroup] [3.5.2] Starting with 2 generic operation threads and 2 partition operation threads.
2015-07-10 13:26:38.531  INFO 5 --- [           main] com.hazelcast.system                     : [10.244.77.3]:5701 [someGroup] [3.5.2] Hazelcast 3.5 (20150617 - 4270dc6) starting at Address[10.244.77.3]:5701
2015-07-10 13:26:38.532  INFO 5 --- [           main] com.hazelcast.system                     : [10.244.77.3]:5701 [someGroup] [3.5.2] Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
2015-07-10 13:26:38.533  INFO 5 --- [           main] com.hazelcast.instance.Node              : [10.244.77.3]:5701 [someGroup] [3.5.2] Creating TcpIpJoiner
2015-07-10 13:26:38.534  INFO 5 --- [           main] com.hazelcast.core.LifecycleService      : [10.244.77.3]:5701 [someGroup] [3.5.2] Address[10.244.77.3]:5701 is STARTING
2015-07-10 13:26:38.672  INFO 5 --- [        cached1] com.hazelcast.nio.tcp.SocketConnector    : [10.244.77.3]:5701 [someGroup] [3.5.2] Connecting to /10.244.37.3:5701, timeout: 0, bind-any: true
2015-07-10 13:26:38.683  INFO 5 --- [        cached1] c.h.nio.tcp.TcpIpConnectionManager       : [10.244.77.3]:5701 [someGroup] [3.5.2] Established socket connection between /10.244.77.3:59951
2015-07-10 13:26:45.699  INFO 5 --- [ration.thread-1] com.hazelcast.cluster.ClusterService     : [10.244.77.3]:5701 [someGroup] [3.5.2]

Members [2] {
	Member [10.244.37.3]:5701
	Member [10.244.77.3]:5701 this
}

2015-07-10 13:26:47.722  INFO 5 --- [           main] com.hazelcast.core.LifecycleService      : [10.244.77.3]:5701 [someGroup] [3.5.2] Address[10.244.77.3]:5701 is STARTED
2015-07-10 13:26:47.723  INFO 5 --- [           main] com.github.pires.hazelcast.Application   : Started Application in 13.792 seconds (JVM running for 14.542)
```

Now let's scale our cluster to 4 nodes:
```sh
$ kubectl scale rc hazelcast --replicas=4
```

Examine the status again by checking a node’s log and you should see the 4 members connected.

### tl; dr;
For those of you who are impatient, here is the summary of the commands we ran in this tutorial.

```sh
# create a service to track all hazelcast nodes
kubectl create -f hazelcast-service.yaml

# create a replication controller to replicate hazelcast nodes
kubectl create -f hazelcast-controller.yaml

# scale up to 2 nodes
kubectl scale rc hazelcast --replicas=2

# scale up to 4 nodes
kubectl scale rc hazelcast --replicas=4
```

### Hazelcast Discovery Source

```java
package com.github.pires.hazelcast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

/**
 * Read from Kubernetes API all Hazelcast service bound pods, get their IP and connect to them.
 */
@Controller
public class HazelcastDiscoveryController implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(
      HazelcastDiscoveryController.class);

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Address {
    public String ip;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Subset {
    public List<Address> addresses;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Endpoints {
    public List<Subset> subsets;
  }

  private static String getServiceAccountToken() throws IOException {
    String file = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    return new String(Files.readAllBytes(Paths.get(file)));
  }

  private static String getEnvOrDefault(String var, String def) {
    final String val = System.getenv(var);
    return (val == null || val.isEmpty())
        ? def
        : val;
  }

  // TODO: Load the CA cert when it is available on all platforms.
  private static TrustManager[] trustAll = new TrustManager[] {
    new X509TrustManager() {
      public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      public void checkClientTrusted(X509Certificate[] certs, String authType) {}
      public X509Certificate[] getAcceptedIssuers() { return null; }
    }
  };
  private static HostnameVerifier trustAllHosts = new HostnameVerifier() {
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  };

  @Override
  public void run(String... args) {
    final String serviceName = getEnvOrDefault("HAZELCAST_SERVICE", "hazelcast");
    final String path = "/api/v1/namespaces/default/endpoints/";
    final String domain = getEnvOrDefault("DNS_DOMAIN", "cluster.local");
    final String host = getEnvOrDefault("KUBERNETES_MASTER", "https://kubernetes.default.svc.".concat(domain));
    log.info("Asking k8s registry at {}..", host);

    final List<String> hazelcastEndpoints = new CopyOnWriteArrayList<>();

    try {
      String token = getServiceAccountToken();

      SSLContext ctx = SSLContext.getInstance("SSL");
      ctx.init(null, trustAll, new SecureRandom());

      URL url = new URL(host + path + serviceName);
      HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
      // TODO: remove this when and replace with CA cert loading, when the CA is propogated
      // to all nodes on all platforms.
      conn.setSSLSocketFactory(ctx.getSocketFactory());
      conn.setHostnameVerifier(trustAllHosts);
      conn.addRequestProperty("Authorization", "Bearer " + token);

      ObjectMapper mapper = new ObjectMapper();
      Endpoints endpoints = mapper.readValue(conn.getInputStream(), Endpoints.class);
      if (endpoints != null) {
        if (endpoints.subsets != null && !endpoints.subsets.isEmpty()) {
          endpoints.subsets.parallelStream().forEach(subset -> {
            subset.addresses.parallelStream().forEach(
                addr -> hazelcastEndpoints.add(addr.ip));
          });
        }
      }
    } catch (IOException | NoSuchAlgorithmException | KeyManagementException ex) {
      log.warn("Request to Kubernetes API failed", ex);
    }

    log.info("Found {} pods running Hazelcast.", hazelcastEndpoints.size());

    runHazelcast(hazelcastEndpoints);
  }

  private void runHazelcast(final List<String> nodes) {
    // configure Hazelcast instance
    final Config cfg = new Config();
    cfg.setInstanceName(UUID.randomUUID().toString());
    // group configuration
    final String HC_GROUP_NAME = getEnvOrDefault("HC_GROUP_NAME", "someGroup");
    final String HC_GROUP_PASSWORD = getEnvOrDefault("HC_GROUP_PASSWORD",
        "someSecret");
    final int HC_PORT = Integer.parseInt(getEnvOrDefault("HC_PORT", "5701"));
    cfg.setGroupConfig(new GroupConfig(HC_GROUP_NAME, HC_GROUP_PASSWORD));
    // network configuration initialization
    final NetworkConfig netCfg = new NetworkConfig();
    netCfg.setPortAutoIncrement(false);
    netCfg.setPort(HC_PORT);
    // multicast
    final MulticastConfig mcCfg = new MulticastConfig();
    mcCfg.setEnabled(false);
    // tcp
    final TcpIpConfig tcpCfg = new TcpIpConfig();
    nodes.parallelStream().forEach(tcpCfg::addMember);
    tcpCfg.setEnabled(true);
    // network join configuration
    final JoinConfig joinCfg = new JoinConfig();
    joinCfg.setMulticastConfig(mcCfg);
    joinCfg.setTcpIpConfig(tcpCfg);
    netCfg.setJoin(joinCfg);
    // ssl
    netCfg.setSSLConfig(new SSLConfig().setEnabled(false));
    // set it all
    cfg.setNetworkConfig(netCfg);
    // run
    Hazelcast.newHazelcastInstance(cfg);
  }

}

```

/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.pires.hazelcast;

import static com.github.pires.hazelcast.Constants.hazelcastPodLabelKey;
import static com.github.pires.hazelcast.Constants.hazelcastPodLabelValue;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.model.PodSchema;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read from Kubernetes API all annotated Hazelcast pods, get their IP and connect to them.
 */
public class Main {
  
  private static final Logger log = LoggerFactory.getLogger(Main.class);
  
  private static Kubernetes kubernetes;
  
  public static void main(String... args) {
    log.info("Asking k8s registry at {}..",
        KubernetesFactory.DEFAULT_KUBERNETES_MASTER);
    KubernetesFactory kubernetesFactory = new KubernetesFactory(
        KubernetesFactory.DEFAULT_KUBERNETES_MASTER);
    final List<PodSchema> hazelcastPods = retrieveHazelcasPods(
        kubernetesFactory.
        createKubernetes());
    log.info("Found {} pods running Hazelcast.", hazelcastPods.size());
  }
  
  public static List<PodSchema> retrieveHazelcasPods(final Kubernetes kubernetes) {
    final List<PodSchema> hazelcastPods = new CopyOnWriteArrayList<>();
    // TODO map.get may return null
    kubernetes.getPods().getItems().parallelStream().filter(pod -> pod.
        getLabels().get(hazelcastPodLabelKey).equals(hazelcastPodLabelValue)).
        forEach(hazelcastPods::add);
    return hazelcastPods;
  }
  
}

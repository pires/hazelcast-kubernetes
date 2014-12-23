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
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.model.PodSchema;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

/**
 * Read from Kubernetes API all labeled Hazelcast pods, get their IP and connect to them.
 */
@Controller
public class HazelcastDiscoveryController implements CommandLineRunner {
  
  private static final Logger log = LoggerFactory.getLogger(
      HazelcastDiscoveryController.class);

  // TODO load this from env vars
  private static final String HC_GROUP_NAME = "someGroup";
  private static final String HC_GROUP_PASSWORD = "someSecret";
  private static final int HC_PORT = 5701;
  
  @Value("#{systemEnvironment.KUBERNETES_RO_SERVICE_HOST}")
  private String kubeMasterHost;
  
  @Value("#{systemEnvironment.KUBERNETES_RO_SERVICE_PORT}")
  private String kubeMasterPort;
  
  private String getKubeApi() {
    return "http://" + kubeMasterHost + ":" + kubeMasterPort;
  }
  
  @Override
  public void run(String... args) {
    log.info("Asking k8s registry at {}..", getKubeApi());
    KubernetesFactory kubernetesFactory = new KubernetesFactory(getKubeApi());
    final List<PodSchema> hazelcastPods = retrieveHazelcasPods(
        kubernetesFactory.createKubernetes());
    log.info("Found {} pods running Hazelcast.", hazelcastPods.size());
    if (!hazelcastPods.isEmpty()) {
      runHazelcast(hazelcastPods);
    }
  }
  
  public List<PodSchema> retrieveHazelcasPods(final Kubernetes kubernetes) {
    final List<PodSchema> hazelcastPods = new CopyOnWriteArrayList<>();
    kubernetes.getPods().getItems().parallelStream().filter(pod -> pod.
        getLabels().get(hazelcastPodLabelKey).equals(hazelcastPodLabelValue)).
        forEach(hazelcastPods::add);
    return hazelcastPods;
  }
  
  private void runHazelcast(final List<PodSchema> hazelcastPods) {
    // configure Hazelcast instance
    final Config cfg = new Config();
    cfg.setInstanceName(UUID.randomUUID().toString());
    // group configuration
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
    hazelcastPods.stream().filter(
        pod -> pod.getCurrentState().getPodIP() != null).forEach(pod -> {
          final String podIp = pod.getCurrentState().getPodIP();
          tcpCfg.addMember(podIp);
          log.info("Added member {}", podIp);
        });
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

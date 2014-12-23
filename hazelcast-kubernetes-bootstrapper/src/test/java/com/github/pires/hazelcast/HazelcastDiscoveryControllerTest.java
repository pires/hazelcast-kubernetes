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
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import java.util.HashMap;
import java.util.Map;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;
import org.powermock.api.easymock.annotation.Mock;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HazelcastDiscoveryControllerTest {

  @Mock
  private Kubernetes kubernetes;

  private final HazelcastDiscoveryController controller = new HazelcastDiscoveryController();

  @BeforeClass
  public void setup() throws Exception {
    kubernetes = createMock(Kubernetes.class);
  }

  @BeforeMethod
  public void beforeMethod() {
    reset(kubernetes);
  }

  @Test
  public void test_found_hazelcast_pods() {
    prepare_kubernetes_pod_request();
    replay(kubernetes);
    assertEquals(2, controller.retrieveHazelcasPods(kubernetes).size());
    verify(kubernetes);
  }

  private Map<String, String> prepareLabels(boolean valid) {
    final Map<String, String> podLabels = new HashMap<>();
    if (valid) {
      podLabels.put(hazelcastPodLabelKey, hazelcastPodLabelValue);
    } else {
      podLabels.put(hazelcastPodLabelKey, "not_hazelcast");
    }

    return podLabels;
  }

  private void prepare_kubernetes_pod_request() {
    final PodSchema pod1 = new PodSchema();
    pod1.setId("test1");
    pod1.setLabels(prepareLabels(false));

    final PodSchema pod2 = new PodSchema();
    pod2.setLabels(prepareLabels(false));

    final PodSchema podHC1 = new PodSchema();
    podHC1.setLabels(prepareLabels(true));
    final PodSchema podHC2 = new PodSchema();
    podHC2.setLabels(prepareLabels(true));

    final PodListSchema podSchema = new PodListSchema();
    podSchema.getItems().add(pod1);
    podSchema.getItems().add(pod2);
    podSchema.getItems().add(podHC1);
    podSchema.getItems().add(podHC2);

    expect(kubernetes.getPods()).andReturn(podSchema).anyTimes();
  }

}

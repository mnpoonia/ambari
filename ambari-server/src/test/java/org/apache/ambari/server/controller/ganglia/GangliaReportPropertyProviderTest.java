/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.ganglia;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.ComponentSSLConfigurationTest;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test the Ganglia report property provider.
 */
@RunWith(Parameterized.class)
public class GangliaReportPropertyProviderTest {

  private static final String PROPERTY_ID = PropertyHelper.getPropertyId("metrics/load", "Procs");
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Clusters", "cluster_name");

  private ComponentSSLConfiguration configuration;

  @Parameterized.Parameters
  public static Collection<Object[]> configs() {
    ComponentSSLConfiguration configuration1 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", false, false);

    ComponentSSLConfiguration configuration2 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", true, false);

    ComponentSSLConfiguration configuration3 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", false, true);

    return Arrays.asList(new Object[][]{
        {configuration1},
        {configuration2},
        {configuration3}
    });
  }

  public GangliaReportPropertyProviderTest(ComponentSSLConfiguration configuration) {
    this.configuration = configuration;
  }

  @Test
  public void testPopulateResources() throws Exception {

    TestStreamProvider streamProvider    = new TestStreamProvider("temporal_ganglia_report_data.json");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaReportPropertyProvider propertyProvider = new GangliaReportPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.Cluster),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    String expected = (configuration.isGangliaSSL() ? "https" : "http") + "://domU-12-31-39-0E-34-E1.compute-1.internal/ganglia/graph.php?g=load_report&json=1";
    Assert.assertEquals(expected, streamProvider.getLastSpec());

    Assert.assertEquals(2, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID));
  }

  private static class TestGangliaHostProvider implements GangliaHostProvider {

    @Override
    public String getGangliaCollectorHostName(String clusterName) {
      return "domU-12-31-39-0E-34-E1.compute-1.internal";
    }
  }
}

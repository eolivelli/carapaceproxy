/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package org.carapaceproxy.server.cache;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import org.carapaceproxy.EndpointStats;
import org.carapaceproxy.client.ConnectionsManagerStats;
import org.carapaceproxy.client.EndpointKey;
import org.carapaceproxy.server.HttpProxyServer;
import org.carapaceproxy.utils.RawHttpClient;
import org.carapaceproxy.utils.TestEndpointMapper;
import org.carapaceproxy.utils.TestUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author francesco.caliumi
 */
public class CacheContentLengthLimitTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void testWithContentLenghtHeader() throws Exception {

        String body = "01234567890123456789";

        stubFor(get(urlEqualTo("/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withHeader("Content-Length", body.length() + "")
                        .withBody(body)));

        testFileSizeCache(body, false);
    }

    @Test
    public void testWithoutContentLenghtHeader() throws Exception {

        String body = "01234567890123456789";

        stubFor(get(urlEqualTo("/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(body)));

        testFileSizeCache(body, true);
    }

    private void requestAndTestCached(
            String body, boolean chunked, EndpointKey key, HttpProxyServer server, boolean cached, int cacheSize) throws IOException {

        int port = server.getLocalPort();
        ConnectionsManagerStats stats = server.getConnectionsManager().getStats();

        try (RawHttpClient client = new RawHttpClient("localhost", port)) {
            RawHttpClient.HttpResponse resp = client.executeRequest("GET /index.html HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");
            String s = resp.toString();
            System.out.println("s:" + s);
            if (!chunked) {
                assertTrue(s.endsWith(body));
            } else {
                assertTrue(s.endsWith(
                        Integer.toString(body.length(), 16) + "\r\n"
                        + body + "\r\n"
                        + "0\r\n\r\n"));
            }
            assertThat(resp.getHeaderLines().stream().anyMatch(h -> h.contains("X-Cached")), is(cached));

            assertNotNull(stats.getEndpoints().get(key));
            assertThat(server.getCache().getCacheSize(), is(cacheSize));
        }
    }

    private void testFileSizeCache(String body, boolean chunked) throws Exception {

        TestEndpointMapper mapper = new TestEndpointMapper("localhost", wireMockRule.port(), true);
        EndpointKey key = new EndpointKey("localhost", wireMockRule.port());

        // No size checking
        {
            ConnectionsManagerStats stats;

            try (HttpProxyServer server = HttpProxyServer.buildForTests("localhost", 0, mapper);) {
                server.getCurrentConfiguration().setCacheMaxFileSize(0);
                server.getCache().reloadConfiguration(server.getCurrentConfiguration());
                server.start();

                stats = server.getConnectionsManager().getStats();

                // First request
                requestAndTestCached(body, chunked, key, server, false, 1);

                // Should be cached
                requestAndTestCached(body, chunked, key, server, true, 1);
            }

            TestUtils.waitForCondition(() -> {
                EndpointStats epstats = stats.getEndpointStats(key);
                return epstats.getTotalConnections().intValue() == 1
                        && epstats.getActiveConnections().intValue() == 0
                        && epstats.getOpenConnections().intValue() == 0;
            });

            TestUtils.waitForCondition(TestUtils.ALL_CONNECTIONS_CLOSED(stats));
        }

        // Max size set to current content size
        {
            ConnectionsManagerStats stats;

            try (HttpProxyServer server = HttpProxyServer.buildForTests("localhost", 0, mapper);) {
                server.getCurrentConfiguration().setCacheMaxFileSize(body.length());
                server.getCache().reloadConfiguration(server.getCurrentConfiguration());
                server.start();

                stats = server.getConnectionsManager().getStats();

                // First request
                requestAndTestCached(body, chunked, key, server, false, 1);

                // Should be cached
                requestAndTestCached(body, chunked, key, server, true, 1);
            }

            TestUtils.waitForCondition(() -> {
                EndpointStats epstats = stats.getEndpointStats(key);
                return epstats.getTotalConnections().intValue() == 1
                        && epstats.getActiveConnections().intValue() == 0
                        && epstats.getOpenConnections().intValue() == 0;
            });

            TestUtils.waitForCondition(TestUtils.ALL_CONNECTIONS_CLOSED(stats));
        }

        // Max size set to drop current content
        {
            ConnectionsManagerStats stats;

            try (HttpProxyServer server = HttpProxyServer.buildForTests("localhost", 0, mapper);) {
                server.getCurrentConfiguration().setCacheMaxFileSize(body.length() - 1);
                server.getCache().reloadConfiguration(server.getCurrentConfiguration());
                server.start();

                stats = server.getConnectionsManager().getStats();

                // First request
                requestAndTestCached(body, chunked, key, server, false, 0);

                // Should not be cached
                requestAndTestCached(body, chunked, key, server, false, 0);
            }

            TestUtils.waitForCondition(() -> {
                EndpointStats epstats = stats.getEndpointStats(key);
                return epstats.getTotalConnections().intValue() == 1
                        && epstats.getActiveConnections().intValue() == 0
                        && epstats.getOpenConnections().intValue() == 0;
            });

            TestUtils.waitForCondition(TestUtils.ALL_CONNECTIONS_CLOSED(stats));
        }
    }

}

/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.ds.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.CrawlingConfig;
import org.codelibs.fess.opensearch.config.exentity.FailureUrl;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.SystemHelper;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Unit tests for {@link ExampleDataStore}.
 *
 * <p>
 * These tests run inside a UTFlute Lasta Di container (see {@link UnitDsTestCase})
 * and use a {@link TestIndexUpdateCallback} that collects the stored
 * {@code dataMap}s instead of a mocking framework. This mirrors how the real Fess
 * data store plugins are tested and demonstrates the scriptMap-based field
 * mapping end-to-end.
 * </p>
 */
public class ExampleDataStoreTest extends UnitDsTestCase {

    public ExampleDataStore dataStore;

    @Override
    public void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        dataStore = new ExampleDataStore();

        // storeData uses CrawlerStatsHelper (which in turn uses SystemHelper).
        // Register initialized instances so the full pipeline can run in the test.
        ComponentUtil.register(new SystemHelper(), "systemHelper");
        final CrawlerStatsHelper crawlerStatsHelper = new CrawlerStatsHelper();
        crawlerStatsHelper.init();
        ComponentUtil.register(crawlerStatsHelper, "crawlerStatsHelper");

        // On the error/abort path the data store records the failure via
        // FailureUrlService. The real implementation needs OpenSearch, so register a
        // no-op stub to keep this a self-contained unit test. It is registered under
        // the class canonical name so that ComponentUtil.getComponent(Class) resolves
        // it when the container's auto-scanned bean cannot be bound.
        ComponentUtil.register(new FailureUrlService() {
            @Override
            public FailureUrl store(final CrawlingConfig crawlingConfig, final String errorName, final String url, final Throwable e) {
                return null;
            }
        }, FailureUrlService.class.getCanonicalName());
    }

    @Test
    public void test_getName() {
        assertEquals("ExampleDataStore", dataStore.getName());
    }

    @Test
    public void test_storeData_defaultSize() {
        final TestIndexUpdateCallback callback = new TestIndexUpdateCallback();
        final DataStoreParams paramMap = new DataStoreParams();

        dataStore.storeData(new DataConfig(), callback, paramMap, defaultScriptMap(), new HashMap<>());

        // No data.size parameter -> default number of records.
        assertEquals(ExampleDataStore.DEFAULT_DATA_SIZE, callback.getDataMapList().size());
    }

    @Test
    public void test_storeData_customSize() {
        final TestIndexUpdateCallback callback = new TestIndexUpdateCallback();
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("data.size", "5");

        dataStore.storeData(new DataConfig(), callback, paramMap, defaultScriptMap(), new HashMap<>());

        assertEquals(5, callback.getDataMapList().size());
    }

    @Test
    public void test_storeData_zeroSize() {
        final TestIndexUpdateCallback callback = new TestIndexUpdateCallback();
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("data.size", "0");

        dataStore.storeData(new DataConfig(), callback, paramMap, defaultScriptMap(), new HashMap<>());

        assertEquals(0, callback.getDataMapList().size());
    }

    @Test
    public void test_storeData_fieldMapping() {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final TestIndexUpdateCallback callback = new TestIndexUpdateCallback();
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("data.size", "1");

        // The scriptMap defines how source fields map to index fields. Each value is
        // a script template that is evaluated against the resultMap (params + source).
        final Map<String, String> scriptMap = new HashMap<>();
        scriptMap.put(fessConfig.getIndexFieldTitle(), "title");
        scriptMap.put(fessConfig.getIndexFieldContent(), "body");
        scriptMap.put(fessConfig.getIndexFieldUrl(), "url");

        dataStore.storeData(new DataConfig(), callback, paramMap, scriptMap, new HashMap<>());

        assertEquals(1, callback.getDataMapList().size());
        final Map<String, Object> dataMap = callback.getDataMapList().get(0);
        // Source field "title" -> index field title, etc.
        assertEquals("Sample 0", dataMap.get(fessConfig.getIndexFieldTitle()));
        assertEquals("Sample body text for record 0", dataMap.get(fessConfig.getIndexFieldContent()));
        assertEquals("http://fess.codelibs.org/?sample=0", dataMap.get(fessConfig.getIndexFieldUrl()));
    }

    @Test
    public void test_storeData_defaultDataMapIsCopied() {
        final TestIndexUpdateCallback callback = new TestIndexUpdateCallback();
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("data.size", "2");

        final Map<String, Object> defaultDataMap = new HashMap<>();
        defaultDataMap.put("mimetype", "application/datastore");

        dataStore.storeData(new DataConfig(), callback, paramMap, defaultScriptMap(), defaultDataMap);

        assertEquals(2, callback.getDataMapList().size());
        for (final Map<String, Object> dataMap : callback.getDataMapList()) {
            assertEquals("application/datastore", dataMap.get("mimetype"));
        }
    }

    @Test
    public void test_storeData_abortStopsLoop() {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        // This callback aborts on the second stored document.
        final TestIndexUpdateCallback callback = new TestIndexUpdateCallback() {
            @Override
            public void store(final DataStoreParams paramMap, final Map<String, Object> dataMap) {
                super.store(paramMap, dataMap);
                if (getDataMapList().size() == 2) {
                    throw new DataStoreCrawlingException((String) dataMap.get(fessConfig.getIndexFieldUrl()), "aborted", null, true);
                }
            }
        };
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("data.size", "10");

        final Map<String, String> scriptMap = new HashMap<>();
        scriptMap.put(fessConfig.getIndexFieldUrl(), "url");

        dataStore.storeData(new DataConfig(), callback, paramMap, scriptMap, new HashMap<>());

        // The loop should stop after the aborted record; no further records processed.
        assertEquals(2, callback.getDataMapList().size());
    }

    private Map<String, String> defaultScriptMap() {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final Map<String, String> scriptMap = new HashMap<>();
        scriptMap.put(fessConfig.getIndexFieldTitle(), "title");
        scriptMap.put(fessConfig.getIndexFieldContent(), "body");
        scriptMap.put(fessConfig.getIndexFieldUrl(), "url");
        return scriptMap;
    }

    /**
     * Test implementation of {@link IndexUpdateCallback} that collects every stored
     * {@code dataMap} so that assertions can be made on the indexed documents.
     */
    private static class TestIndexUpdateCallback implements IndexUpdateCallback {
        private final List<Map<String, Object>> dataMapList = new ArrayList<>();

        @Override
        public void store(final DataStoreParams paramMap, final Map<String, Object> dataMap) {
            dataMapList.add(new HashMap<>(dataMap));
        }

        @Override
        public long getExecuteTime() {
            return 0;
        }

        @Override
        public long getDocumentSize() {
            return dataMapList.size();
        }

        @Override
        public void commit() {
            // nothing
        }

        public List<Map<String, Object>> getDataMapList() {
            return dataMapList;
        }
    }
}

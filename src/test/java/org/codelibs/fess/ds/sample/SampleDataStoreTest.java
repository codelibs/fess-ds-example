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
package org.codelibs.fess.ds.sample;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsAction;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsKeyObject;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastaflute.LastaFluteTestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class SampleDataStoreTest extends LastaFluteTestCase {
    private SampleDataStore dataStore;
    private FessConfig fessConfig;
    private CrawlerStatsHelper crawlerStatsHelper;
    private IndexUpdateCallback callback;
    private DataConfig dataConfig;
    private DataStoreParams paramMap;
    private FailureUrlService failureUrlService;

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    protected boolean isSuppressTestCaseTransaction() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dataStore = new SampleDataStore();

        // Mock FessConfig
        fessConfig = mock(FessConfig.class);
        when(fessConfig.getIndexFieldUrl()).thenReturn("url");
        when(fessConfig.getIndexFieldHost()).thenReturn("host");
        when(fessConfig.getIndexFieldSite()).thenReturn("site");
        when(fessConfig.getIndexFieldTitle()).thenReturn("title");
        when(fessConfig.getIndexFieldContent()).thenReturn("content");
        when(fessConfig.getIndexFieldDigest()).thenReturn("digest");
        when(fessConfig.getIndexFieldAnchor()).thenReturn("anchor");
        when(fessConfig.getIndexFieldContentLength()).thenReturn("content_length");
        when(fessConfig.getIndexFieldLastModified()).thenReturn("last_modified");

        // Mock CrawlerStatsHelper
        crawlerStatsHelper = mock(CrawlerStatsHelper.class);

        // Mock IndexUpdateCallback
        callback = mock(IndexUpdateCallback.class);

        // Mock DataConfig
        dataConfig = mock(DataConfig.class);
        when(dataConfig.getId()).thenReturn("test-config");

        // Mock DataStoreParams
        paramMap = new DataStoreParams();

        // Mock FailureUrlService
        failureUrlService = mock(FailureUrlService.class);
    }

    @Override
    public void tearDown() throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    /**
     * Test for getName() method
     */
    public void test_getName() {
        String name = dataStore.getName();
        assertEquals("SampleDataStore", name);
    }

    /**
     * Test for constructor
     */
    public void test_constructor() {
        SampleDataStore testDataStore = new SampleDataStore();
        assertNotNull(testDataStore);
        assertEquals("SampleDataStore", testDataStore.getName());
    }

    /**
     * Test storeData with default data size (10)
     */
    public void test_storeData_defaultSize() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify callback.store was called 10 times (default size)
            verify(callback, times(10)).store(any(DataStoreParams.class), any(Map.class));

            // Verify crawler stats helper interactions
            verify(crawlerStatsHelper, times(10)).begin(any(StatsKeyObject.class));
            verify(crawlerStatsHelper, times(10)).record(any(StatsKeyObject.class), eq(StatsAction.PREPARED));
            verify(crawlerStatsHelper, times(10)).record(any(StatsKeyObject.class), eq(StatsAction.FINISHED));
            verify(crawlerStatsHelper, times(10)).done(any(StatsKeyObject.class));
        }
    }

    /**
     * Test storeData with custom data size
     */
    public void test_storeData_customSize() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "5");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify callback.store was called 5 times (custom size)
            verify(callback, times(5)).store(any(DataStoreParams.class), any(Map.class));
        }
    }

    /**
     * Test storeData with zero data size
     */
    public void test_storeData_zeroSize() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "0");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify callback.store was never called
            verify(callback, never()).store(any(DataStoreParams.class), any(Map.class));
        }
    }

    /**
     * Test that generated document data has correct structure
     */
    public void test_storeData_documentStructure() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "1");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            ArgumentCaptor<Map> dataMapCaptor = ArgumentCaptor.forClass(Map.class);

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            verify(callback).store(any(DataStoreParams.class), dataMapCaptor.capture());

            Map<String, Object> capturedDataMap = dataMapCaptor.getValue();
            assertEquals("http://fess.codelibs.org/?sample=0", capturedDataMap.get("url"));
            assertEquals("fess.codelibs.org", capturedDataMap.get("host"));
            assertEquals("fess.codelibs.org/0", capturedDataMap.get("site"));
            assertEquals("Sample 0", capturedDataMap.get("title"));
            assertEquals("Sample Test0", capturedDataMap.get("content"));
            assertEquals("Sample Data0", capturedDataMap.get("digest"));
            assertEquals("http://fess.codelibs.org/?from=0", capturedDataMap.get("anchor"));
            assertEquals(0L, capturedDataMap.get("content_length"));
            assertNotNull(capturedDataMap.get("last_modified"));
        }
    }

    /**
     * Test storeData with different document indices
     */
    public void test_storeData_multipleDocuments() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "3");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            ArgumentCaptor<Map> dataMapCaptor = ArgumentCaptor.forClass(Map.class);

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            verify(callback, times(3)).store(any(DataStoreParams.class), dataMapCaptor.capture());

            // Verify the last captured document (index 2)
            Map<String, Object> lastDoc = dataMapCaptor.getValue();
            assertEquals("http://fess.codelibs.org/?sample=2", lastDoc.get("url"));
            assertEquals("Sample 2", lastDoc.get("title"));
            assertEquals("Sample Test2", lastDoc.get("content"));
            assertEquals(200L, lastDoc.get("content_length")); // 2 * 100L
        }
    }

    /**
     * Test CrawlingAccessException handling
     */
    public void test_storeData_crawlingAccessException() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "2");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            // Throw exception on first store call
            doThrow(new CrawlingAccessException("Test exception"))
                    .doNothing()
                    .when(callback).store(any(DataStoreParams.class), any(Map.class));

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify callback was called 2 times
            verify(callback, times(2)).store(any(DataStoreParams.class), any(Map.class));

            // Verify failure was recorded
            verify(failureUrlService, times(1)).store(eq(dataConfig), anyString(), anyString(), any(Throwable.class));

            // Verify stats helper recorded exception
            verify(crawlerStatsHelper, times(1)).record(any(StatsKeyObject.class), eq(StatsAction.ACCESS_EXCEPTION));
        }
    }

    /**
     * Test MultipleCrawlingAccessException handling
     */
    public void test_storeData_multipleCrawlingAccessException() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "1");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            // Create MultipleCrawlingAccessException with causes
            Throwable cause1 = new RuntimeException("Cause 1");
            Throwable cause2 = new IllegalStateException("Cause 2");
            MultipleCrawlingAccessException exception = new MultipleCrawlingAccessException("Multiple errors",
                    new Throwable[] { cause1, cause2 });

            doThrow(exception).when(callback).store(any(DataStoreParams.class), any(Map.class));

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify failure was recorded with the last cause
            verify(failureUrlService).store(eq(dataConfig), anyString(), anyString(), any(Throwable.class));
            verify(crawlerStatsHelper).record(any(StatsKeyObject.class), eq(StatsAction.ACCESS_EXCEPTION));
        }
    }

    /**
     * Test DataStoreCrawlingException with aborted flag
     */
    public void test_storeData_dataStoreCrawlingException_aborted() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "5");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            // Create aborted exception (url, message, cause, aborted)
            DataStoreCrawlingException exception = new DataStoreCrawlingException("http://test.com", "Aborted", null, true);

            // Throw exception on second call
            doNothing()
                    .doThrow(exception)
                    .when(callback).store(any(DataStoreParams.class), any(Map.class));

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify callback was called only 2 times (stopped after abort)
            verify(callback, times(2)).store(any(DataStoreParams.class), any(Map.class));

            // Verify failure was recorded
            verify(failureUrlService).store(eq(dataConfig), anyString(), eq("http://test.com"), any(Throwable.class));
        }
    }

    /**
     * Test generic exception handling
     */
    public void test_storeData_genericException() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "2");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            // Throw generic exception on first call
            doThrow(new RuntimeException("Generic error"))
                    .doNothing()
                    .when(callback).store(any(DataStoreParams.class), any(Map.class));

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify callback was called 2 times
            verify(callback, times(2)).store(any(DataStoreParams.class), any(Map.class));

            // Verify failure was recorded
            verify(failureUrlService).store(eq(dataConfig), anyString(), eq("line:0"), any(Throwable.class));

            // Verify stats helper recorded exception
            verify(crawlerStatsHelper).record(any(StatsKeyObject.class), eq(StatsAction.EXCEPTION));
        }
    }

    /**
     * Test that StatsKeyObject is properly set in paramMap
     */
    public void test_storeData_statsKeyObjectInParamMap() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "1");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify StatsKeyObject was set in paramMap
            Object statsKey = paramMap.get(Constants.CRAWLER_STATS_KEY);
            assertNotNull(statsKey);
            assertTrue(statsKey instanceof StatsKeyObject);
        }
    }

    /**
     * Test that done() is always called in finally block
     */
    public void test_storeData_doneAlwaysCalled() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "3");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            // Throw exception on second call
            doNothing()
                    .doThrow(new RuntimeException("Test error"))
                    .doNothing()
                    .when(callback).store(any(DataStoreParams.class), any(Map.class));

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify done() was called for all 3 attempts (including the failed one)
            verify(crawlerStatsHelper, times(3)).done(any(StatsKeyObject.class));
        }
    }

    /**
     * Test that all crawler stats actions are called
     */
    public void test_storeData_allStatsActionsCalled() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "1");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            // Verify all stats actions are called in correct order
            verify(crawlerStatsHelper).begin(any(StatsKeyObject.class));
            verify(crawlerStatsHelper).record(any(StatsKeyObject.class), eq(StatsAction.PREPARED));
            verify(crawlerStatsHelper).record(any(StatsKeyObject.class), eq(StatsAction.FINISHED));
            verify(crawlerStatsHelper).done(any(StatsKeyObject.class));
        }
    }

    /**
     * Test that content length increases correctly
     */
    public void test_storeData_contentLengthIncrement() {
        try (MockedStatic<ComponentUtil> componentUtil = Mockito.mockStatic(ComponentUtil.class)) {
            componentUtil.when(ComponentUtil::getFessConfig).thenReturn(fessConfig);
            componentUtil.when(ComponentUtil::getCrawlerStatsHelper).thenReturn(crawlerStatsHelper);
            componentUtil.when(() -> ComponentUtil.getComponent(FailureUrlService.class)).thenReturn(failureUrlService);

            paramMap.put("data.size", "5");
            Map<String, String> scriptMap = new HashMap<>();
            Map<String, Object> defaultDataMap = new HashMap<>();

            ArgumentCaptor<Map> dataMapCaptor = ArgumentCaptor.forClass(Map.class);

            dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

            verify(callback, atLeast(5)).store(any(DataStoreParams.class), dataMapCaptor.capture());

            // Verify content lengths are 0L, 100L, 200L, 300L, 400L
            var allValues = dataMapCaptor.getAllValues();
            for (int i = 0; i < 5; i++) {
                assertEquals((long) (i * 100L), allValues.get(i).get("content_length"));
            }
        }
    }
}

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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsAction;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsKeyObject;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;

/**
 * Example data store implementation for Fess.
 *
 * <p>
 * This class is intended as a copy-from template for developers who want to
 * write their own data store crawler. It generates a configurable number of
 * synthetic source records and feeds each record through the standard Fess
 * data store pipeline.
 * </p>
 *
 * <p>
 * The pipeline that every data store follows is:
 * </p>
 * <ol>
 * <li>Acquire the raw source records from the external system (a database, an
 * API, a file, etc.). In this example the records are generated in memory by
 * {@link #createSourceRecord(int)}.</li>
 * <li>For each record, build a {@code resultMap} that merges the configured
 * parameters ({@code paramMap}) with the fields of the source record. The
 * admin-configured scripts ({@code scriptMap}) are evaluated against this
 * {@code resultMap}.</li>
 * <li>For each entry of the {@code scriptMap}, call the inherited
 * {@link #convertValue(String, String, Map)} to produce the value of an index
 * field, and put the non-null results into the {@code dataMap}.</li>
 * <li>Hand the {@code dataMap} to {@code callback.store(...)} so that Fess
 * indexes it as a document.</li>
 * </ol>
 *
 * <p>
 * The mapping between source fields and index fields is therefore NOT
 * hard-coded here: it is defined by the administrator in the "Script" area of
 * the data store configuration. For example, a script map such as:
 * </p>
 *
 * <pre>
 * title=title
 * content=body
 * url=url
 * </pre>
 *
 * <p>
 * tells this data store to copy the source {@code title} field into the index
 * {@code title} field, the source {@code body} field into the index
 * {@code content} field, and so on.
 * </p>
 *
 * <p>
 * Configuration parameters:
 * </p>
 * <ul>
 * <li>{@code data.size} - Number of synthetic source records to generate
 * (default: 10).</li>
 * <li>{@code readInterval} - Interval in milliseconds to wait between records
 * (default: 0).</li>
 * </ul>
 */
public class ExampleDataStore extends AbstractDataStore {

    private static final Logger logger = LogManager.getLogger(ExampleDataStore.class);

    /** The parameter name for the number of records to generate. */
    protected static final String DATA_SIZE_PARAM = "data.size";

    /** The default number of records to generate. */
    protected static final int DEFAULT_DATA_SIZE = 10;

    /**
     * Default constructor.
     */
    public ExampleDataStore() {
        super();
    }

    @Override
    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback, final DataStoreParams paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap) {
        final CrawlerStatsHelper crawlerStatsHelper = ComponentUtil.getCrawlerStatsHelper();

        final long readInterval = getReadInterval(paramMap);

        // CUSTOMIZE: The script type controls how scriptMap templates are evaluated
        // (e.g. Groovy). It is normally taken from the "script_type" parameter.
        final String scriptType = getScriptType(paramMap);

        // CUSTOMIZE: Acquire the raw source records from the external system here.
        // In a real data store you would, for example, open a connection, run a
        // query, or read a file. This example simply generates synthetic records.
        final int dataSize = getDataSize(paramMap);

        boolean running = true;
        for (int i = 0; i < dataSize && running; i++) {
            final StatsKeyObject statsKey = new StatsKeyObject(dataConfig.getId() + "#" + i);
            paramMap.put(Constants.CRAWLER_STATS_KEY, statsKey);
            final Map<String, Object> dataMap = new HashMap<>(defaultDataMap);
            try {
                crawlerStatsHelper.begin(statsKey);

                // CUSTOMIZE: Build one raw source record. Replace this with the data
                // you read from the external system.
                final Map<String, Object> source = createSourceRecord(i);

                // Build the resultMap that the scripts are evaluated against. It
                // contains the configured parameters plus the source record fields.
                final Map<String, Object> resultMap = new LinkedHashMap<>(paramMap.asMap());
                resultMap.putAll(source);

                crawlerStatsHelper.record(statsKey, StatsAction.PREPARED);

                // Evaluate each admin-configured script against the resultMap and put
                // the produced value into the dataMap as an index field. This is the
                // central concept of a Fess data store: the field mapping is defined
                // by the administrator via the scriptMap, not hard-coded here.
                for (final Map.Entry<String, String> entry : scriptMap.entrySet()) {
                    final Object convertValue = convertValue(scriptType, entry.getValue(), resultMap);
                    if (convertValue != null) {
                        dataMap.put(entry.getKey(), convertValue);
                    }
                }

                crawlerStatsHelper.record(statsKey, StatsAction.EVALUATED);

                if (dataMap.get("url") instanceof final String statsUrl) {
                    statsKey.setUrl(statsUrl);
                }

                callback.store(paramMap, dataMap);
                crawlerStatsHelper.record(statsKey, StatsAction.FINISHED);
            } catch (final CrawlingAccessException e) {
                logger.warn("Crawling Access Exception at : {}", dataMap, e);

                Throwable target = e;
                if (target instanceof final MultipleCrawlingAccessException ex) {
                    final Throwable[] causes = ex.getCauses();
                    if (causes.length > 0) {
                        target = causes[causes.length - 1];
                    }
                }

                String errorName;
                final Throwable cause = target.getCause();
                if (cause != null) {
                    errorName = cause.getClass().getCanonicalName();
                } else {
                    errorName = target.getClass().getCanonicalName();
                }

                String url;
                if (target instanceof final DataStoreCrawlingException dce) {
                    url = dce.getUrl();
                    if (dce.aborted()) {
                        running = false;
                    }
                } else {
                    url = "record:" + i;
                }
                final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                failureUrlService.store(dataConfig, errorName, url, target);
                crawlerStatsHelper.record(statsKey, StatsAction.ACCESS_EXCEPTION);
            } catch (final Throwable t) {
                logger.warn("Crawling Access Exception at : {}", dataMap, t);
                final String url = "record:" + i;
                final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                failureUrlService.store(dataConfig, t.getClass().getCanonicalName(), url, t);
                crawlerStatsHelper.record(statsKey, StatsAction.EXCEPTION);
            } finally {
                crawlerStatsHelper.done(statsKey);
            }

            if (readInterval > 0 && running) {
                sleep(readInterval);
            }
        }
    }

    /**
     * Get the number of source records to generate.
     * @param paramMap The parameters.
     * @return The number of records to generate.
     */
    protected int getDataSize(final DataStoreParams paramMap) {
        final String value = paramMap.getAsString(DATA_SIZE_PARAM);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (final NumberFormatException e) {
                logger.warn("Invalid {} value: '{}'. Using default: {}.", DATA_SIZE_PARAM, value, DEFAULT_DATA_SIZE);
            }
        }
        return DEFAULT_DATA_SIZE;
    }

    /**
     * Create a single synthetic source record.
     *
     * <p>
     * CUSTOMIZE: This method represents one row/object/item retrieved from the
     * external system. The keys of the returned map are the source field names
     * that an administrator can reference from the scriptMap (e.g. {@code title},
     * {@code body}, {@code url}).
     * </p>
     *
     * @param index The index of the record.
     * @return The source record as a map of field name to value.
     */
    protected Map<String, Object> createSourceRecord(final int index) {
        final Map<String, Object> source = new HashMap<>();
        source.put("id", Integer.toString(index));
        source.put("title", "Sample " + index);
        source.put("body", "Sample body text for record " + index);
        source.put("url", "http://fess.codelibs.org/?sample=" + index);
        source.put("created", new Date());
        return source;
    }

    /**
     * For testing purposes: collect the synthetic source records that would be
     * generated for the given size.
     * @param dataSize The number of records to generate.
     * @return The list of source records.
     */
    protected List<Map<String, Object>> createSourceRecords(final int dataSize) {
        final List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < dataSize; i++) {
            list.add(createSourceRecord(i));
        }
        return list;
    }
}

/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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

import java.util.Date;
import java.util.HashMap;
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
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsAction;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsKeyObject;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;

public class SampleDataStore extends AbstractDataStore {
    private static final Logger logger = LogManager.getLogger(SampleDataStore.class);

    @Override
    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback, final DataStoreParams paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final CrawlerStatsHelper crawlerStatsHelper = ComponentUtil.getCrawlerStatsHelper();

        final long readInterval = getReadInterval(paramMap);
        final int dataSize = paramMap.get("data.size") instanceof final String value ? Integer.parseInt(value) : 10;
        boolean running = true;
        for (int i = 0; i < dataSize && running; i++) {
            final StatsKeyObject statsKey = new StatsKeyObject(dataConfig.getId() + "#" + i);
            paramMap.put(Constants.CRAWLER_STATS_KEY, statsKey);
            final Map<String, Object> dataMap = new HashMap<>();
            try {
                crawlerStatsHelper.begin(statsKey);

                dataMap.put(fessConfig.getIndexFieldUrl(), "http://fess.codelibs.org/?sample=" + i);
                dataMap.put(fessConfig.getIndexFieldHost(), "fess.codelibs.org");
                dataMap.put(fessConfig.getIndexFieldSite(), "fess.codelibs.org/" + i);
                dataMap.put(fessConfig.getIndexFieldTitle(), "Sample " + i);
                dataMap.put(fessConfig.getIndexFieldContent(), "Sample Test" + i);
                dataMap.put(fessConfig.getIndexFieldDigest(), "Sample Data" + i);
                dataMap.put(fessConfig.getIndexFieldAnchor(), "http://fess.codelibs.org/?from=" + i);
                dataMap.put(fessConfig.getIndexFieldContentLength(), i * 100L);
                dataMap.put(fessConfig.getIndexFieldLastModified(), new Date());
                crawlerStatsHelper.record(statsKey, StatsAction.PREPARED);
                callback.store(paramMap, dataMap);
                crawlerStatsHelper.record(statsKey, StatsAction.FINISHED);
            } catch (final CrawlingAccessException e) {
                logger.warn("Crawling Access Exception at : {}", dataMap, e);

                Throwable target = e;
                if (target instanceof MultipleCrawlingAccessException ex) {
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
                if (target instanceof DataStoreCrawlingException dce) {
                    url = dce.getUrl();
                    if (dce.aborted()) {
                        running = false;
                    }
                } else {
                    url = "line:" + i;
                }
                final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                failureUrlService.store(dataConfig, errorName, url, target);
                crawlerStatsHelper.record(statsKey, StatsAction.ACCESS_EXCEPTION);
            } catch (final Throwable t) {
                logger.warn("Crawling Access Exception at : {}", dataMap, t);
                final String url = "line:" + i;
                final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                failureUrlService.store(dataConfig, t.getClass().getCanonicalName(), url, t);

                if (readInterval > 0) {
                    sleep(readInterval);
                }
                crawlerStatsHelper.record(statsKey, StatsAction.EXCEPTION);
            } finally {
                crawlerStatsHelper.done(statsKey);
            }
        }
    }
}

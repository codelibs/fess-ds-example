/*
 * Copyright 2012-2018 CodeLibs Project and the Others.
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
package org.codelibs.fess.ds.sample

import org.codelibs.fess.app.service.FailureUrlService
import org.codelibs.fess.crawler.exception.CrawlingAccessException
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException
import org.codelibs.fess.ds.AbstractDataStore
import org.codelibs.fess.ds.callback.IndexUpdateCallback
import org.codelibs.fess.es.config.exentity.DataConfig
import org.codelibs.fess.exception.DataStoreCrawlingException
import org.codelibs.fess.util.ComponentUtil
import org.slf4j.LoggerFactory
import java.util.*

class SampleDataStore : AbstractDataStore() {

    companion object {
        private val logger = LoggerFactory.getLogger(SampleDataStore::class.java)
    }

    override fun getName(): String = "Sample"

    override fun storeData(dataConfig: DataConfig, callback: IndexUpdateCallback, paramMap: Map<String, String>,
                           scriptMap: Map<String, String>, defaultDataMap: Map<String, Any>) {
        val fessConfig = ComponentUtil.getFessConfig()

        val readInterval = getReadInterval(paramMap)
        val dataSize = paramMap.getOrDefault("data.size", "10").toInt()
        var running = true
        var i = 0
        while (i < dataSize && running) {
            val dataMap = HashMap<String, Any>()
            try {
                dataMap[fessConfig.indexFieldUrl] = "http://fess.codelibs.org/?sample=$i"
                dataMap[fessConfig.indexFieldHost] = "fess.codelibs.org"
                dataMap[fessConfig.indexFieldSite] = "fess.codelibs.org/$i"
                dataMap[fessConfig.indexFieldTitle] = "Sample $i"
                dataMap[fessConfig.indexFieldContent] = "Sample Test$i"
                dataMap[fessConfig.indexFieldDigest] = "Sample Data$i"
                dataMap[fessConfig.indexFieldAnchor] = "http://fess.codelibs.org/?from=$i"
                dataMap[fessConfig.indexFieldContentLength] = i * 100L
                dataMap[fessConfig.indexFieldLastModified] = Date()
                callback.store(paramMap, dataMap)
            } catch (e: CrawlingAccessException) {
                logger.warn("Crawling Access Exception at : $dataMap", e)

                var target: Throwable = e
                if (target is MultipleCrawlingAccessException) {
                    val causes = target.causes
                    if (causes.isNotEmpty()) {
                        target = causes.last()
                    }
                }

                val cause = target.cause
                val errorName =
                        if (cause != null) cause.javaClass.canonicalName
                        else target.javaClass.canonicalName

                val url = if (target is DataStoreCrawlingException) {
                    val dce = target
                    if (dce.aborted()) {
                        running = false
                    }
                    dce.url
                } else {
                    "line:$i"
                }
                val failureUrlService = ComponentUtil.getComponent(FailureUrlService::class.java)
                failureUrlService.store(dataConfig, errorName, url, target)
            } catch (t: Throwable) {
                logger.warn("Crawling Access Exception at : $dataMap", t)
                val url = "line:$i"
                val failureUrlService = ComponentUtil.getComponent(FailureUrlService::class.java)
                failureUrlService.store(dataConfig, t.javaClass.canonicalName, url, t)

                if (readInterval > 0) {
                    sleep(readInterval)
                }
            }

            i++
        }
    }
}

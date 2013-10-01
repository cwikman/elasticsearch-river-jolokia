/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.river.jolokia;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverIndexName;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;
import org.elasticsearch.river.jolokia.support.RiverContext;
import org.elasticsearch.river.jolokia.support.RiverServiceLoader;

/**
 * The Jolokia river
 *
 * @author Christer Wikman, DevCode
 */
public class JolokiaRiver extends AbstractRiverComponent implements River {

    public final static String NAME = "jolokia-river";
    public final static String TYPE = "jolokia";

    private final String strategy;
    private final TimeValue poll;
    
    private final int maxretries;
    private final TimeValue maxretrywait;
    private final String indexName;
    private final String typeName;
    private final int bulkSize;
    private final int maxBulkRequests;
    private final String indexSettings;
    private final String typeMapping;
    private final boolean versioning;
    private final boolean digesting;
    private final boolean acknowledgeBulk;
    private final RiverSource riverSource;
    private final RiverMouth riverMouth;
    private final RiverContext riverContext;
    private final RiverFlow riverFlow;
    private final JolokiaRiverSetting riverSetting;
    private volatile Thread thread;
    private volatile boolean closed;
        
    @SuppressWarnings("unchecked")
	private List<String> nodeToStringList(Object obj, List<String> defaultValue) {
    	if (null != obj && XContentMapValues.isArray(obj)) {
    		List<String> res = new ArrayList<String>();
    		for (Object o : (List<Object>) obj) {
    			res.add(o.toString());
    		}
    		return res;
    	} else
			return defaultValue;
    }    
    
    @Inject
    public JolokiaRiver(RiverName riverName, RiverSettings riverSettings,
                     @RiverIndexName String riverIndexName,
                     Client client) {
        super(riverName, riverSettings);
        // riverIndexName = _river

        Map<String, Object> sourceSettings =
                riverSettings.settings().containsKey(TYPE)
                        ? (Map<String, Object>) riverSettings.settings().get(TYPE)
                        : new HashMap<String, Object>();

                        
        strategy = XContentMapValues.nodeStringValue(sourceSettings.get("strategy"), "simple");

        riverSetting = new JolokiaRiverSetting();                                
        riverSetting.setHosts(nodeToStringList(sourceSettings.get("hosts"), new ArrayList<String>()));
        riverSetting.setUrl(XContentMapValues.nodeStringValue(sourceSettings.get("url"), null));         
        riverSetting.setObjectName(XContentMapValues.nodeStringValue(sourceSettings.get("objectName"), null));
        riverSetting.setAttributes(nodeToStringList(sourceSettings.get("attributes"), new ArrayList<String>())); 
        riverSetting.setLogType(XContentMapValues.nodeStringValue(sourceSettings.get("logType"), null));
        
        poll = XContentMapValues.nodeTimeValue(sourceSettings.get("poll"), TimeValue.timeValueMinutes(1));
        maxretries = XContentMapValues.nodeIntegerValue(sourceSettings.get("max_retries"), 3);
        maxretrywait = TimeValue.parseTimeValue(XContentMapValues.nodeStringValue(sourceSettings.get("max_retries_wait"), "10s"), TimeValue.timeValueMillis(30000));
        digesting = XContentMapValues.nodeBooleanValue(sourceSettings.get("digesting"), Boolean.TRUE);

        Map<String, Object> targetSettings =
                riverSettings.settings().containsKey("index")
                        ? (Map<String, Object>) riverSettings.settings().get("index")
                        : new HashMap<String, Object>();
        indexName = XContentMapValues.nodeStringValue(targetSettings.get("index"), TYPE);
        typeName = XContentMapValues.nodeStringValue(targetSettings.get("type"), TYPE);
        bulkSize = XContentMapValues.nodeIntegerValue(targetSettings.get("bulk_size"), 100);
        maxBulkRequests = XContentMapValues.nodeIntegerValue(targetSettings.get("max_bulk_requests"), 30);
        indexSettings = XContentMapValues.nodeStringValue(targetSettings.get("index_settings"), null);
        typeMapping = XContentMapValues.nodeStringValue(targetSettings.get("type_mapping"), null);
        versioning = XContentMapValues.nodeBooleanValue(sourceSettings.get("versioning"), Boolean.FALSE);
        acknowledgeBulk = XContentMapValues.nodeBooleanValue(sourceSettings.get("acknowledge"), Boolean.FALSE);

        riverSource = RiverServiceLoader.findRiverSource(strategy);
        
        logger.debug("found river source {} for strategy {}", riverSource.getClass().getName(), strategy);
        
        riverSource.setting(riverSetting);
        	

        riverMouth = RiverServiceLoader.findRiverMouth(strategy);
        logger.debug("found river mouth {} for strategy {}", riverMouth.getClass().getName(), strategy);
        riverMouth.indexTemplate(indexName)
                .type(typeName)
                .maxBulkActions(bulkSize)
                .maxConcurrentBulkRequests(maxBulkRequests)
                .acknowledge(acknowledgeBulk)
                .versioning(versioning)
                .client(client);

        riverContext = new RiverContext()
                .riverName(riverName.getName())
                .riverIndexName(riverIndexName)
                .riverSettings(riverSettings.settings())
                .riverSource(riverSource)
                .riverMouth(riverMouth)
                .pollInterval(poll)
                .retries(maxretries)
                .maxRetryWait(maxretrywait)
                .digesting(digesting)
                .contextualize();

        riverFlow = RiverServiceLoader.findRiverFlow(strategy);
        // prepare task for run
        riverFlow.riverContext(riverContext);

        logger.debug("found river flow {} for strategy {}", riverFlow.getClass().getName(), strategy);
    }

    @Override
    public void start() {
        logger.info("starting Jolokia river: hosts [{}], uri [{}], strategy [{}], index [{}]/[{}]",
                riverSetting.getHosts(), riverSetting.getUrl(), strategy, indexName, typeName);
        try {
            riverFlow.startDate(new Date());
            riverMouth.createIndexIfNotExists(indexSettings, typeMapping);
        } catch (Exception e) {
            if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
                riverFlow.startDate(null);
                // that's fine, continue.
            } else if (ExceptionsHelper.unwrapCause(e) instanceof ClusterBlockException) {
                // ok, not recovered yet..., lets start indexing and hope we recover by the first bulk
            } else {
                logger.warn("failed to create index [{}], disabling Jolokia river...", e, indexName);
                return;
            }
        }
        thread = EsExecutors.daemonThreadFactory(settings.globalSettings(), "Jolokia river [" + riverName.name() + '/' + strategy + ']')
                .newThread(riverFlow);
        thread.start();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        logger.info("closing Jolokia river [" + riverName.name() + '/' + strategy + ']');
        if (thread != null) {
            thread.interrupt();
        }
        if (riverFlow != null) {
            riverFlow.abort();
        }
        if (riverSource != null) {
            riverSource.close();
        }
        if (riverMouth != null) {
            riverMouth.close();
        }
        closed = true; // abort only once
    }

    /**
     * Induce a river run once, but in a synchronous manner.
     */
    public void once() {
        if (riverFlow != null) {
            riverFlow.move();
        }
    }

    /**
     * Induce a river run once, but in an asynchronous manner.
     */
    public void induce() {
        RiverFlow riverTask = RiverServiceLoader.findRiverFlow(strategy);
        // prepare task for run
        riverTask.riverContext(riverContext);
        thread = EsExecutors.daemonThreadFactory(settings.globalSettings(), "BeanSpy river (fired) [" + riverName.name() + '/' + strategy + ')').newThread(riverTask);
        riverTask.abort();
        thread.start(); // once
    }

}

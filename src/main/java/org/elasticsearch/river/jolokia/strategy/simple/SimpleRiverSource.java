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
package org.elasticsearch.river.jolokia.strategy.simple;

import java.io.IOException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.http.HttpStatus;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.river.jolokia.JolokiaRiverSetting;
import org.elasticsearch.river.jolokia.RiverSource;
import org.elasticsearch.river.jolokia.support.RiverContext;
import org.elasticsearch.river.jolokia.support.StructuredObject;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;

/**
 * A river source implementation for the 'simple' strategy.
 * <p/>
 * It connects to a Jolokia server, fetches from it by using a merge method
 * provided by the river task.
 * <p/>
 * 
 * @author Christer Wikman, DevCode
 */
public class SimpleRiverSource implements RiverSource {

	private final ESLogger logger = ESLoggerFactory.getLogger(SimpleRiverSource.class.getName());

	protected RiverContext context;

	protected JolokiaRiverSetting setting;

	public SimpleRiverSource() {
	}

	@Override
	public String strategy() {
		return "simple";
	}

	@Override
	public SimpleRiverSource riverContext(RiverContext context) {
		this.context = context;
		return this;
	}

	@Override
	public String fetch() {
		for (String host : setting.getHosts()) {
			fetch(host);
		}
		return null;
	}

	private static final String FIELD_SEPERATOR = ".";
	private static final String FIELD_PREFIX = "@fields" + FIELD_SEPERATOR;

	private static final String ERROR_PREFIX = FIELD_PREFIX + "error" + FIELD_SEPERATOR;

	private static final String TIMESTAMP = "@timestamp";
	private static final String REQUEST_URL = FIELD_PREFIX + "request";
	private static final String RESPONSE = FIELD_PREFIX + "response";
	private static final String ERROR = FIELD_PREFIX + "error";
	private static final String ERROR_TYPE = FIELD_PREFIX + "error_type";

	private static final String OBJECT_NAME = FIELD_PREFIX + "objectName";
	private static final String SOURCE_HOST = "@source_host";
	private static final String TYPE = "@type";

	// private static final String SOURCE_HOST_GRP = "@source_host_grp";
	// private static final String TAGS = "@tags";

	private String[] getAttributes() {
		try {
			String[] attribs = setting.getAttributes().toArray(new String[] {});
			return attribs;
		} catch (Exception e) {
			return new String[] {};
		}
	}

	private void createReading(StructuredObject reading) {
		try {
			context.riverMouth().create(reading);
		} catch (Exception e) {
			logger.error("Failed to create document for " + reading, e);
		}
	}

	private String getObjectName(ObjectName o) {
		return o.getDomain() + ":" + o.getCanonicalName();
	}

	private String getUrl(String hostname) {
		return String.format(setting.getUrl(), hostname);
	}

	private StructuredObject createReading(String hostname, String objectName) {
		StructuredObject reading = new StructuredObject();
		reading.source(TIMESTAMP, new DateTime().toDateTimeISO().toString());
		reading.source(SOURCE_HOST, hostname);
		reading.source(REQUEST_URL, getUrl(hostname));
		reading.source(RESPONSE, HttpStatus.SC_OK);
		reading.source(OBJECT_NAME, objectName);
		reading.source(TYPE, setting.getLogType());
		return reading;
	}

	public void fetch(String hostname) {
		String url = "?";
		String objectName = "?";
		String[] attributes = new String[] {};
		try {
			url = getUrl(hostname);
			objectName = setting.getObjectName();
			attributes = getAttributes();

			J4pClient j4pClient = new J4pClient(url);
			J4pReadRequest req = new J4pReadRequest(objectName, attributes);

			logger.info("Executing {}, {}, {}", url, objectName, attributes);
			J4pReadResponse resp = j4pClient.execute(req);

			for (ObjectName object : resp.getObjectNames()) {
				StructuredObject reading = createReading(hostname, getObjectName(object));
				for (String attrib : attributes) {
					try {
						Object v = resp.getValue(object, attrib);
						reading.source(FIELD_PREFIX + attrib, v);
					} catch (Exception e) {
						reading.source(ERROR_PREFIX + attrib, e.getMessage());
					}
				}
				createReading(reading);
			}
		} catch (Exception e) {
			try {
				logger.info("Failed to execute request {} {} {}", url, objectName, attributes, e);
				StructuredObject reading = createReading(hostname, setting.getObjectName());
				reading.source(ERROR_TYPE, e.getClass().getName());
				reading.source(ERROR, e.getMessage());
				int rc = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				if (e instanceof J4pRemoteException) {
					rc = ((J4pRemoteException) e).getStatus();
				}
				reading.source(RESPONSE, rc);
				createReading(reading);
			} catch (Exception e1) {
				logger.error("Failed to store error message", e1);
			}
		}
	}

	/**
	 * Send acknowledge command if exists.
	 */
	public void acknowledge() {
	}

	@Override
	public RiverSource acknowledge(BulkResponse response) throws IOException {
		return this;
	}

	@Override
	public RiverSource close() {
		return this;
	}

	public RiverSource setting(JolokiaRiverSetting setting) {
		this.setting = setting;
		return this;
	}
}
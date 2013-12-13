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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.http.HttpStatus;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.river.jolokia.JolokiaRiverSetting;
import org.elasticsearch.river.jolokia.JolokiaRiverSetting.Attribute;
import org.elasticsearch.river.jolokia.RiverSource;
import org.elasticsearch.river.jolokia.support.RiverContext;
import org.elasticsearch.river.jolokia.support.StructuredObject;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.json.simple.JSONValue;

import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeObject;

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

	private static final String ERROR = "error";
	
	private static final String ERROR_PREFIX = ERROR + FIELD_SEPERATOR;

	private static final String TIMESTAMP = "@timestamp";
	private static final String REQUEST_URL = "request";
	private static final String RESPONSE = "response";
	
	private static final String ERROR_TYPE = "error_type";

	private static final String OBJECT_NAME = "objectName";
	private static final String HOST = "host";
	private static final String FULL_HOST = "fqdn";
	private static final String DOMAIN = "domain";
	private static final String TYPE = "type";

	// private static final String HOST_GRP = "host_grp";
	// private static final String TAGS = "tags";

	private String[] getAttributeNames() {
		try {
			String[] attribs = new String[setting.getAttributes().size()];
			for(int i =0; i<setting.getAttributes().size(); i++) {
				attribs[i] = setting.getAttributes().get(i).getName();
			}
			return attribs;
		} catch (Exception e) {
			return new String[] {};
		}
	}
	
	private Map<String, String> getAttributeTransforms() {
		try {
			Map<String, String> mappings = new HashMap<String, String>();
			for(Attribute attr : setting.getAttributes()) {
				if (null != attr.getTransform()) {
					mappings.put(attr.getName(), attr.getTransform());
				}
			}
			return mappings;
		} catch (Exception e) {
			return new HashMap<String, String>();
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
		reading.source(HOST, getHostPart(hostname));
		reading.source(FULL_HOST, hostname);
		reading.source(DOMAIN, getDomainPart(hostname));
		reading.source(REQUEST_URL, getUrl(hostname));
		reading.source(RESPONSE, HttpStatus.SC_OK);
		reading.source(OBJECT_NAME, objectName);
		reading.source(TYPE, setting.getLogType());
		return reading;
	}

	public void fetch(String hostname) {
		String url = "?";
		String objectName = "?";
		String[] attributeNames = new String[] {};
		try {
			String host = getHost(hostname);
			String port = getPort(hostname);
			String userName = getUser(hostname);
			String password = getPassword(hostname);
			url = getUrl(null==port?host:(host + ":" + port));
			objectName = setting.getObjectName();
			Map<String, String> transforms = getAttributeTransforms();
			
			attributeNames = getAttributeNames();

			J4pClient j4pClient = useBasicAuth(userName, password) ? J4pClient.url(url)
					.user(userName)
					.password(password)
					.build() : J4pClient.url(url).build();

			J4pReadRequest req = new J4pReadRequest(objectName, attributeNames);

			logger.info("Executing {}, {}, {}", url, objectName, attributeNames);
			J4pReadResponse resp = j4pClient.execute(req);
			
			if (setting.getOnlyUpdates() && null != resp.asJSONObject().get("value")) {
				Integer oldValue = setting.getLastValueAsHash();
				setting.setLastValueAsHash(resp.asJSONObject().get("value").toString().hashCode());
				if (null != oldValue && oldValue.equals(setting.getLastValueAsHash())) {
					logger.info("Skipping " + objectName + " since no values has changed");
					return;
				}
			}

			ScriptEngineManager manager = new ScriptEngineManager();
	        ScriptEngine engine = manager.getEngineByName("rhino");
			
			for (ObjectName object : resp.getObjectNames()) {
				StructuredObject reading = createReading(host, getObjectName(object));
				for (String attrib : attributeNames) {
					try {
						Object v = resp.getValue(object, attrib);
						
						// Transform
						if(transforms.containsKey(attrib)) {							
							String function = transforms.get(attrib).replaceFirst("^\\s*function\\s+([^\\s\\(]+)\\s*\\(.*$" , "$1");
							engine.eval(transforms.get(attrib));
							v = convert(engine.eval(function+"("+JSONValue.toJSONString(v)+")"));
						}
						
						reading.source(attrib, v);
					} catch (Exception e) {
						reading.source(ERROR_PREFIX + attrib, e.getMessage());
					}
				}
				createReading(reading);
			}
		} catch (Exception e) {
			try {
				logger.info("Failed to execute request {} {} {}", url, objectName, attributeNames, e);
				StructuredObject reading = createReading(getHost(hostname), setting.getObjectName());
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

	private boolean useBasicAuth(String userName, String password) {
		return null != userName && null != password;
	}

	private String getPassword(String hostname) {
		String[] parts = hostname.split("@");
		if(parts.length>1) {
			String[] authParts = parts[0].split(":");
			if (authParts.length>1) 
				return authParts[1];
		}
		return null;
	}

	private String getUser(String hostname) {
		String[] parts = hostname.split("@");
		if(parts.length>1) {
			return parts[0].split(":")[0];
		}
		return null;
	}

	private String getHost(String hostname) {
		String[] parts = hostname.split("@");
		if(parts.length>1) {
			return parts[1].split(":")[0];
		}
		return hostname.split(":")[0];
	}
	
	private String getDomainPart(String hostname) {
		String[] parts = hostname.split("\\.");
		if(parts.length>1) {
			return join(".", Arrays.copyOfRange(parts, 1, parts.length));
		}
		return "";
	}
	
	private static String join(String del, String[] array) {
		if(array.length>1) {
			return array[0] + del + join(del, Arrays.copyOfRange(array, 1, array.length));
		}
		return array[0];
	}
	
	private String getHostPart(String hostname) {
		return hostname.split("\\.")[0];
	}
	
	private String getPort(String hostname) {
		String[] hostParts;
		String[] parts = hostname.split("@");
		if(parts.length>1) {
			hostParts = parts[1].split(":");
		}
		else {
			hostParts = hostname.split(":");
		}
		if (hostParts.length>1) { 
			return hostParts[1];
		}
		else {
			return null;
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
	
	/**
	 * Converts NativeArray and NativeObject recursively
	 * 
	 * @param object the native object
	 * @return converted object
	 */
	private Object convert(Object object) {
		if (object instanceof NativeObject) {
			return convertObject((NativeObject) object);
		} else if (object instanceof NativeArray) {
			return convertArray((NativeArray) object);
		} else {
			return object;
		}
	}
	
	private Map<String, Object> convertObject(NativeObject object) {
		Map<String, Object> mapOutput = new HashMap<String, Object>();
		for (Object o : object.getIds()) {
			String key = (String) o;
			mapOutput.put(key, convert(object.get(key, null)));
		}
		
		return mapOutput;
	}
	
	private List<Object> convertArray(NativeArray array) {
		List<Object> listOutput = new ArrayList<Object>();
		for (Object o : array.getIds()) {
		    int index = (Integer) o;
		    listOutput.add(convert(array.get(index, null)));
		}
		return listOutput;
	}
}
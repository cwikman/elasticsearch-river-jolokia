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

import java.io.IOException;

import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.river.jolokia.support.RiverContext;

/**
 * The river source models the data producing side
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 * @author Christer Wikman, DevCode
 */
public interface RiverSource {

	/**
	 * The strategy this river source supports.
	 * 
	 * @return the strategy as a string
	 */
	String strategy();

	/**
	 * Set the river context
	 * 
	 * @param context
	 *            the context
	 * @return this river source
	 */
	RiverSource riverContext(RiverContext context);

	/**
	 * Fetch a data portion from the source and pass it to the river task for
	 * further processing.
	 * 
	 * @return a checksum of the fetched data or null
	 * @throws IOException
	 */
	String fetch() throws IOException;


	/** Settings */
	RiverSource setting(JolokiaRiverSetting setting);
	
	/**
	 * Acknowledge a bulk response
	 * 
	 * @param response
	 * @return this river source
	 * @throws IOException
	 */
	RiverSource acknowledge(BulkResponse response) throws IOException;

	/** Closes all resources opened by the river source. */
	RiverSource close();

}

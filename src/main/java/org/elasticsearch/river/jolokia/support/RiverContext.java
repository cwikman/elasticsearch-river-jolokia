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
package org.elasticsearch.river.jolokia.support;

import java.util.Map;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.river.jolokia.RiverMouth;
import org.elasticsearch.river.jolokia.RiverSource;

/**
 * The river context consists of the parameters that span over source and target, and the source and target.
 * It represents the river state, for supporting the river task, and river scripting.
 *
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class RiverContext {

    /**
     * The name of the river.
     */
    private String name;
    /**
     * The river index name
     */
    private String riverIndexName;
    /**
     * The settings of the river
     */
    private Map<String, Object> settings;
    /**
     * The source of the river
     */
    private RiverSource source;
    /**
     * The target of the river
     */
    private RiverMouth mouth;
    /**
     * The polling interval
     */
    private TimeValue poll;

    /**
     * The job name of the current river task
     */
    private String job;
    /**
     * The maximum number of rows per statement execution
     */
    private int maxRows;
    /**
     * The number of retries
     */
    private int retries;
    /**
     * The time to wait between retries
     */
    private TimeValue maxretrywait;

    /**
     * If digesting should be used or not
     */
    private boolean digesting;

    public RiverContext riverSettings(Map<String, Object> settings) {
        this.settings = settings;
        return this;
    }

    public Map<String, Object> riverSettings() {
        return settings;
    }

    public RiverContext riverIndexName(String name) {
        this.riverIndexName = name;
        return this;
    }

    public String riverIndexName() {
        return riverIndexName;
    }

    public RiverContext riverName(String name) {
        this.name = name;
        return this;
    }

    public String riverName() {
        return name;
    }

    public RiverContext riverSource(RiverSource source) {
        this.source = source;
        return this;
    }

    public RiverSource riverSource() {
        return source;
    }

    public RiverContext riverMouth(RiverMouth mouth) {
        this.mouth = mouth;
        return this;
    }

    public RiverMouth riverMouth() {
        return mouth;
    }

    public RiverContext job(String job) {
        this.job = job;
        return this;
    }

    public String job() {
        return job;
    }

    public RiverContext pollInterval(TimeValue poll) {
        this.poll = poll;
        return this;
    }

    public TimeValue pollingInterval() {
        return poll;
    }

    public RiverContext maxRows(int maxRows) {
        this.maxRows = maxRows;
        return this;
    }

    public int maxRows() {
        return maxRows;
    }

    public RiverContext retries(int retries) {
        this.retries = retries;
        return this;
    }

    public int retries() {
        return retries;
    }

    public RiverContext maxRetryWait(TimeValue maxretrywait) {
        this.maxretrywait = maxretrywait;
        return this;
    }

    public TimeValue maxRetryWait() {
        return maxretrywait;
    }

    public RiverContext digesting(boolean digesting) {
        this.digesting = digesting;
        return this;
    }

    public boolean digesting() {
        return digesting;
    }

    public RiverContext contextualize() {
        if (source != null) {
            source.riverContext(this);
        }
        if (mouth != null) {
            mouth.riverContext(this);
        }
        return this;
    }
}

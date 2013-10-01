package org.elasticsearch.river.jolokia.support;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Date;

import org.elasticsearch.river.jolokia.support.StructuredObject;
import org.junit.Test;


public class StructuredObjectTest {

	@Test
	public void testBuild() throws IOException {
    	StructuredObject reading = new StructuredObject();
    	reading.source("@timestamp", new Date());
    	reading.source("@fields.request", "URL");    	
		reading.source("@fields.response", 200);
		String res = reading.build();
		assertNotNull(res);
	}

}

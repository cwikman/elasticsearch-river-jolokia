package org.elasticsearch.river.jolokia.strategy.simple;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class SimpleRiverMouthTest {

	@Test
	public void testGetRealIndex() {
		String index = "logstash.[yyyy-MM-dd]";
		SimpleRiverMouth mouth = new SimpleRiverMouth();
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd"); 
		assertEquals("logstash."+fmt.format(new Date()), mouth.getRealIndex(index));
	}

}

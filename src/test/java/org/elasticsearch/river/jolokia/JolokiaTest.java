package org.elasticsearch.river.jolokia;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pExecResponse;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.junit.Test;

public class JolokiaTest {

	@Test
	public void testReadData() throws Exception {
		J4pClient j4pClient = new J4pClient("http://localhost:8080/jolokia-war-1.1.3");
		// J4pReadRequest req = new
		// J4pReadRequest("jboss.system:type=ServerInfo","ActiveThreadCount",
		// "ActiveThreadGroupCount", "MaxMemory", "HostName",
		// "TotalMemory","FreeMemory");
		List<String> attributes = Arrays.asList(new String[] { "Status" });
		//
		// J4pReadRequest req = new
		// J4pReadRequest("jboss.ws:context=*,endpoint=*",
		// attributes.toArray(new String[]{}));
	
		try {
			J4pReadRequest req = new J4pReadRequest("af-probe:probe=*", attributes.toArray(new String[] {}));
			
			J4pReadResponse resp = j4pClient.execute(req);
			System.out.println(resp.getValue());
	
			for (ObjectName o : resp.getObjectNames()) {
				for (String attrib : attributes) {
					try {
						Object v = resp.getValue(o, attrib);
						System.out.println(o.getDomain() + " " + o.getCanonicalName() + " " + attrib + "=" + v);
					} catch (Exception ignore) {
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	@Test
	public void testExecData() throws Exception {
		J4pClient j4pClient = new J4pClient("http://localhost:8080/jolokia-war-1.1.3");
		// J4pReadRequest req = new
		// J4pReadRequest("jboss.system:type=ServerInfo","ActiveThreadCount",
		// "ActiveThreadGroupCount", "MaxMemory", "HostName",
		// "TotalMemory","FreeMemory");
		List<String> attributes = Arrays.asList(new String[] { "DetailedStatus" });
		//
		// J4pReadRequest req = new
		// J4pReadRequest("jboss.ws:context=*,endpoint=*",
		// attributes.toArray(new String[]{}));

		J4pExecRequest req = new J4pExecRequest("se.arbetsformedlingen.utils.probe:service=TestWebService2", "checkStatus");

		J4pExecResponse resp = j4pClient.execute(req);
		Map<String, Object> v = (Map<String, Object>) resp.getValue();
		System.out.println(v);
		for (String attrib : attributes) {
			try {
				System.out.println(attrib + "=" + v.get(attrib));
			} catch (Exception ignore) {
			}
		}

	}

}

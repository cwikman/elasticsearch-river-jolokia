package org.elasticsearch.river.jolokia;

import java.util.List;

public class JolokiaRiverSetting {
	
	/**
	 * List of hosts if you want to query the same MBean on several servers
	 * 
	 * @return this river source
	 */	
	private List<String> hosts;
	
	/**
	 * URL to Jolokia. It supports template url, e.g. http://%s:8080/jolokia, to
	 * query several hosts from the same config
	 * 
	 * @param url
	 *            e.g. http://%s:8080/jolokia
	 * @return this river source
	 */	
	private String url;
	
	/**
	 * The ObjectName of the MBean for which the attribute should be fetched. It
	 * contains two parts: A domain part and a list of properties which are
	 * separated by :. Properties themselves are combined in a comma separated
	 * list of key-value pairs. This name can be a pattern in which case
	 * multiple MBeans are queried for the attribute value.
	 * 
	 * @param objectName e.g. "jboss.system:type=ServerInfo"
	 * @return this river source
	 */	
	private String objectName;
	
	
	/**
	 * This can be a list of Attribute names to be fetched
	 * 
	 * @param attributes
	 *            e.g. "TotalMemory","FreeMemory"
	 * @return this river source
	 */	
	private List<String> attributes;
	
	/** Type of log reading, e.g. Probe, DbConnection, etc */
	private String logType;
	
	public String getLogType() {
		return logType;
	}
	public void setLogType(String logType) {
		this.logType = logType;
	}
	public List<String> getHosts() {
		return hosts;
	}
	public void setHosts(List<String> hosts) {
		this.hosts = hosts;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getObjectName() {
		return objectName;
	}
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}
	public List<String> getAttributes() {
		return attributes;
	}
	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}
	
	

}

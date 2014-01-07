Elasticsearch Jolokia JMX river
==========================================

The Jolokia river indexes JMX values exposed as JSON by [Jolokia](http://www.jolokia.org) into [Elasticsearch](http://www.elasticsearch.org>).

It is implemented as an [Elasticsearch plugin](<http://www.elasticsearch.org/guide/reference/modules/plugins.html>]) 
and it based on the [JDBC river](<https://github.com/jprante/elasticsearch-river-jdbc>`). 

JMX values can be monitored and analysed in `Kibana <http://www.elasticsearch.org/overview/kibana/>`_.


Creating a Jolokia river is easy:
```
curl -XPUT localhost:9200/_river/jmx_mem_probe/_meta -d '
{
    "type" : "jolokia",
    "jolokia" : {
    	"hosts" : ["admin:password@localhost:8080"],
    	"url" : "http://%s/jolokia",
        "objectName" : "java.lang:type=java.lang",
        "attributes": [ "FreePhysicalMemorySize", "TotalPhysicalMemorySize", "FreeSwapSpaceSize", 
                        {"name"="TotalSwapSpaceSize", transform="function trans(input) {return input;}"}, 
                        "CommittedVirtualMemorySize" ],
        "constants": { "host_group" : "webservers" },
        "logType" : "mem_probe",
        "poll" : "1m",
        "onlyUpdates" : "true"
    },
    "index" : {
        "index" : "logstash-[yyyy.MM.dd]",
        "type" : "MemProbe"
    }
}'
```

Installation
------------

Prerequisites::

* Elasticsearch +0.90.3
* Jolokia deployed on your app servers

Version | Release date | Command 
--- | --- | ---
 1.0.0  | October 1, 2013 | ./bin/plugin -u file:elasticsearch-river-jolokia-1.0.1.zip -i river-jolokia 


Bintray:

https://bintray.com/pkg/show/general/cwikman/elasticsearch-plugins/elasticsearch-river-jolokia


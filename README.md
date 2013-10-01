Elasticsearch Jolokia JMX river
==========================================

The Jolokia river indexes JMX values exposed as JSON by `Jolokia <http://www.jolokia.org>` into `Elasticsearch <http://www.elasticsearch.org>`.

It is implemented as an `Elasticsearch plugin <http://www.elasticsearch.org/guide/reference/modules/plugins.html>`_ and it based on the `JDBC river <https://github.com/jprante/elasticsearch-river-jdbc>`_. 

JMX values can be monitored and analysed in `Kibana <http://www.elasticsearch.org/overview/kibana/>`_.


Creating a Jolokia river is easy::

curl -XPUT localhost:9200/_river/jmx_mem_probe/_meta -d '
{
    "type" : "jolokia",
    "jolokia" : {
        "hosts" : ["localhost"],
        "url" : "http://%s:8080/jolokia",
        "objectName" : "java.lang:type=java.lang",
        "attributes": [ "FreePhysicalMemorySize", "TotalPhysicalMemorySize", "FreeSwapSpaceSize", "TotalSwapSpaceSize", "CommittedVirtualMemorySize" ],
        "logType" : "mem_probe",
        "poll" : "1m"
    },
    "index" : {
        "index" : "logstash-[yyyy.MM.dd]",
        "type" : "MemProbe"
    }
}'


Installation
------------

Prerequisites::

  Elasticsearch +0.90.3
  Jolokia deployed in one or more app server

==========  =================  =============================================================================
Version     Release date       Command
-----_----  -----------------  -----------------------------------------------------------------------------
1.0.0         October 1, 2013  ./bin/plugin -u file:elasticsearch-river-jolokia-1.0.0.zip -i river-jolokia
==========  =================  =============================================================================

Bintray:

https://bintray.com/pkg/show/general/cwikman/elasticsearch-plugins/elasticsearch-river-jolokia


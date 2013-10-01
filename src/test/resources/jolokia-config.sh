export RIVER_HOST="http://172.16.200.141:9200"
export HOSTS='["localhost:8080","jboss01.l7700520.upa.ams.se"]'
export JOLOKIA_URL="http://%s/jolokia"

curl -XPUT $RIVER_HOST/_river/jolokia_server_info/_meta -d '
{
    "type" : "jolokia",
    "jolokia" : {
        "hosts" : '$HOSTS',
        "url" : "'$JOLOKIA_URL'",
        "objectName" : "jboss.system:type=ServerInfo",
        "attributes": [ "ActiveThreadCount", "ActiveThreadGroupCount", "MaxMemory", "HostName", "TotalMemory","FreeMemory" ],
        "tag" : "server_info",
        "poll" : "1m"
    },
    "index" : {
        "index" : "logstash-[yyyy.MM.dd]",
        "type" : "ServerInfo"
    }
}'

curl -XPUT $RIVER_HOST/_river/jolokia_connection_pool/_meta -d '
{
    "type" : "jolokia",
    "jolokia" : {
        "hosts" : '$HOSTS',
        "url" : "'$JOLOKIA_URL'",
        "objectName" : "jboss.jca:name=*,service=ManagedConnectionPool",
        "attributes": ["MaxSize","ConnectionCount","MaxConnectionsInUseCount","ConnectionDestroyedCount","AvailableConnectionCount","ConnectionCreatedCount","InUseConnectionCount"],
        "logType" : "connection_pool",
        "poll" : "1m"
    },
    "index" : {
        "index" : "logstash-[yyyy.MM.dd]",
        "type" : "ConnectionPool"
    }
}'

curl -XPUT $RIVER_HOST/_river/jolokia_globalrequestprocessor/_meta -d '
{
    "type" : "jolokia",
    "jolokia" : {
        "hosts" : '$HOSTS',
        "url" : "'$JOLOKIA_URL'",
        "objectName" : "jboss.web:name=ajp*,type=GlobalRequestProcessor",
        "attributes": ["bytesSent","bytesReceived","processingTime","errorCount","maxTime","requestCount"],
        "logType" : "global_request_processor",
        "poll" : "1m"
    },
    "index" : {
        "index" : "logstash-[yyyy.MM.dd]",
        "type" : "GlobalRequestProcessor"
    }
}'


curl -XPUT $RIVER_HOST/_river/jolokia_thread_pool/_meta -d'
{
    "type" : "jolokia",
    "jolokia" : {
        "hosts" : '$HOSTS',
        "url" : "'$JOLOKIA_URL'",
        "objectName" : "jboss.web:name=ajp*,type=ThreadPool",
        "attributes": ["port", "threadPriority", "maxThreads","backlog", "soTimeout" ,"acceptorThreadCount", "name", "currentThreadsBusy", "pollerSize", "currentThreadCount"],
        "logType" : "thread_pool",
        "poll" : "1m"
    },
    "index" : {
        "index" : "logstash-[yyyy.MM.dd]",
        "type" : "ThreadPool"
    }
}'


curl -XPUT $RIVER_HOST/_river/jolokia_probe/_meta -d '
{
    "type" : "jolokia",
    "jolokia" : {
        "hosts" : '$HOSTS',
        "url" : "'$JOLOKIA_URL'",
        "objectName" : "af-probe:probe=*",
        "attributes": ["Status","DetailedStatus"],
        "logType" : "probe",
        "poll" : "1m"
    },
    "index" : {
        "index" : "logstash-[yyyy.MM.dd]",
        "type" : "Probe"
    }
}'

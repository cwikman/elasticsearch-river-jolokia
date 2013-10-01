
package org.elasticsearch.plugin.river.jolokia;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.JolokiaRiverInduceAction;
import org.elasticsearch.river.RiversModule;
import org.elasticsearch.river.jolokia.JolokiaRiverModule;

/**
 * Jolokia River plugin. This plugin is the enrty point for the
 * Elasticsearch river service. 
 *
 * @author Christer Wikman, DevCode
 */
public class JolokiaRiverPlugin extends AbstractPlugin {

    public final static String NAME = "jolokia-river";
    public final static String TYPE = "jolokia";
    
    @Inject
    public JolokiaRiverPlugin() {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Jolokia River";
    }

    /**
     * Register the river to Elasticsearch node
     *
     * @param module
     */
    public void onModule(RiversModule module) {
        module.registerRiver(TYPE, JolokiaRiverModule.class);
    }

    /**
     * Register the REST move to Elasticsearch node
     *
     * @param module
     */
    public void onModule(RestModule module) {
        module.addRestAction(JolokiaRiverInduceAction.class);
    }

}

package org.elasticsearch.river.jolokia;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.river.River;

/**
 * The Jolokia river module
 *
 * @author Christer Wikman, DevCode
 */
public class JolokiaRiverModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(River.class).to(JolokiaRiver.class).asEagerSingleton();
    }
}

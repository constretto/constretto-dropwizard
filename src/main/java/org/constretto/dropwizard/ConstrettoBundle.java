package org.constretto.dropwizard;

import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.constretto.internal.resolver.DefaultConfigurationContextResolver;
import org.constretto.resolver.ConfigurationContextResolver;

/**
 * Adding this bundle provides support for constretto-based filtering of configuration file.
 *
 * @author kjeivers
 */
public class ConstrettoBundle<T extends Configuration> implements Bundle {

    private final ConfigurationContextResolver tagResolver;

    /**
     * Constretto-support using the DefaultConfigurationContextResolver.
     * This means that the constretto tags is read from the system property 'CONSTRETTO_TAGS'
     * (or the environment variable with the same name)
     *
     * -DCONSTRETTO_TAGS=a,b,c,d
     */
    public ConstrettoBundle() {
        this(new DefaultConfigurationContextResolver());
    }

    /**
     * Constretto-support allowing an arbitrary tag resolver
     * @param tagResolver Provider of the set of active constretto tags
     */
    public ConstrettoBundle(ConfigurationContextResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    /**
     * Rig the configuration source provider and the configuration factory provider
     *
     * @param bootstrap Provides the original ConfigurationSourceProvider
     */
    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Bootstrap bootstrap) {
        bootstrap.setConfigurationSourceProvider(new ConstrettoConfigurationProvider(bootstrap.getConfigurationSourceProvider()));
        bootstrap.setConfigurationFactoryFactory(new ConstrettoConfigurationFactoryFactory<T>(tagResolver));
    }

    @Override
    public void run(Environment environment) {
        // all work is done in initialize()
    }

}

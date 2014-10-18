package org.constretto.dropwizard;

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
public class ConstrettoBundle<T extends Configuration> implements io.dropwizard.ConfiguredBundle<T> {

    private final ConfigurationContextResolver tagResolver;

    public ConstrettoBundle() {
        this(new DefaultConfigurationContextResolver());
    }

    public ConstrettoBundle(ConfigurationContextResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    /**
     * Rig the configuration source provider and the configuration factory provider
     *
     * @param bootstrap Provides the original ConfigurationSourceProvider
     */
    @Override
    public void initialize(Bootstrap bootstrap) {
        bootstrap.setConfigurationSourceProvider(new ConstrettoConfigurationProvider(bootstrap.getConfigurationSourceProvider()));
        bootstrap.setConfigurationFactoryFactory(new ConstrettoConfigurationFactoryFactory<T>(tagResolver));
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        // all work is done in initialize()
    }

}

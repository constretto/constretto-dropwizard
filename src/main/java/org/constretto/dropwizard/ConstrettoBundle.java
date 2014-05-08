package org.constretto.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.constretto.internal.resolver.DefaultConfigurationContextResolver;
import org.constretto.resolver.ConfigurationContextResolver;

/**
 * @author kjeivers@gmail.com
 */
public class ConstrettoBundle<T extends Configuration> implements io.dropwizard.ConfiguredBundle<T> {

    private final ConfigurationContextResolver tagResolver;

    public ConstrettoBundle() {
        this(new DefaultConfigurationContextResolver());
    }

    public ConstrettoBundle(ConfigurationContextResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {

    }

    @Override
    public void initialize(Bootstrap bootstrap) {
        bootstrap.setConfigurationSourceProvider(new ConstrettoConfigurationProvider(bootstrap.getConfigurationSourceProvider()));
        bootstrap.setConfigurationFactoryFactory(new ConstrettoConfigurationFactoryFactory<T>(tagResolver));
    }
}

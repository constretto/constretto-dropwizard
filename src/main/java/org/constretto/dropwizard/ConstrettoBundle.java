package org.constretto.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * @author kjeivers@gmail.com
 */
public class ConstrettoBundle<T extends Configuration> implements io.dropwizard.ConfiguredBundle<T> {
    @Override
    public void run(T configuration, Environment environment) throws Exception {

    }

    @Override
    public void initialize(Bootstrap bootstrap) {
        bootstrap.setConfigurationSourceProvider(new ConstrettoConfigurationProvider());
        bootstrap.setConfigurationFactoryFactory(new ConstrettoConfigurationFactoryFactory<T>());
    }
}

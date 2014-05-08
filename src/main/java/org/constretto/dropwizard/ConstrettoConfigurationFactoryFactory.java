package org.constretto.dropwizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import org.constretto.resolver.ConfigurationContextResolver;

import javax.validation.Validator;

/**
 * @author kjiv
 */
public class ConstrettoConfigurationFactoryFactory<T> implements ConfigurationFactoryFactory<T> {

    private final ConfigurationContextResolver tagResolver;

    public ConstrettoConfigurationFactoryFactory(ConfigurationContextResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    @Override
    public ConfigurationFactory<T> create(Class<T> klass, Validator validator, ObjectMapper objectMapper, String propertyPrefix) {
        return new ConstrettoConfigurationFactory<>(klass, validator, objectMapper, propertyPrefix, tagResolver);
    }
}

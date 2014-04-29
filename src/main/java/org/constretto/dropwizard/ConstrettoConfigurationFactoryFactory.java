package org.constretto.dropwizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;

import javax.validation.Validator;

/**
 * @author kjiv
 */
public class ConstrettoConfigurationFactoryFactory<T> implements ConfigurationFactoryFactory<T> {
    @Override
    public ConfigurationFactory<T> create(Class<T> klass, Validator validator, ObjectMapper objectMapper, String propertyPrefix) {
        return new ConstrettoConfigurationFactory<T>(klass, validator, objectMapper, propertyPrefix);
    }
}

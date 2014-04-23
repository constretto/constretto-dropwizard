package org.constretto.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import org.constretto.resolver.ConfigurationContextResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.validation.Validation;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author kjeivers@gmail.com
 */
public class ConstrettoConfigurationProviderTest {

    ConstrettoConfigurationProvider provider;
    ConfigurationFactory<TestConfiguration> factory;
    ConfigurationContextResolver tagResolver;

    @Before
    public void setup() {
        tagResolver = Mockito.mock(ConfigurationContextResolver.class);
        provider = new ConstrettoConfigurationProvider(tagResolver) {
            @Override
            public InputStream open(String path) throws IOException {
                return super.open(new StringReader(path));
            }
        };
        factory = new ConfigurationFactory<TestConfiguration>(
                TestConfiguration.class,
                Validation.buildDefaultValidatorFactory().getValidator(),
                new ObjectMapper(),
                "");
    }

    @Test
    public void testNoActiveTagsWithRootScalar() throws IOException, ConfigurationException {
        TestConfiguration config = factory.build(provider,
                "scalar: testVal\n" +
                "@staging.scalar: stagingVal"
        );
        assertThat(config).isNotNull();
        assertThat(config.scalar).isEqualTo("testVal");
    }

    @Test
    public void testActiveTagWithRootScalar() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("staging"));
        TestConfiguration config = factory.build(provider,
                "scalar: testVal\n" +
                "@staging.scalar: stagingVal"
        );
        assertThat(config).isNotNull();
        assertThat(config.scalar).isEqualTo("stagingVal");
    }

    public static class TestConfiguration {
        @JsonProperty
        public String scalar;

        @JsonProperty
        public List<String> list;
    }

}

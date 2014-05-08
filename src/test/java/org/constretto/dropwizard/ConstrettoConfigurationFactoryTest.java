package org.constretto.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import org.constretto.resolver.ConfigurationContextResolver;
import org.junit.Before;
import org.junit.Ignore;
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
public class ConstrettoConfigurationFactoryTest {

    ConstrettoConfigurationProvider provider;
    ConfigurationFactory<TestConfiguration> factory;
    ConfigurationContextResolver tagResolver;

    @Before
    public void setup() {
        tagResolver = Mockito.mock(ConfigurationContextResolver.class);
        provider = new ConstrettoConfigurationProvider() {
            @Override
            public InputStream open(String path) throws IOException {
                return super.open(new StringReader(path));
            }
        };
        factory = new ConstrettoConfigurationFactory<>(
                TestConfiguration.class,
                Validation.buildDefaultValidatorFactory().getValidator(),
                new ObjectMapper(),
                "",
                tagResolver);
    }

    @Test
    public void testNoActiveTagsWithRootScalar() throws IOException, ConfigurationException {
        TestConfiguration config = factory.build(provider,
                "scalar: testVal \n" +
                ".staging.scalar: stagingVal"
        );
        assertThat(config).isNotNull();
        assertThat(config.scalar).isEqualTo("testVal");
    }

    @Test
    public void testActiveTagWithRootScalar() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("staging"));
        TestConfiguration config = factory.build(provider,
                "scalar: testVal \n" +
                ".staging.scalar: stagingVal"
        );
        assertThat(config).isNotNull();
        assertThat(config.scalar).isEqualTo("stagingVal");
    }

    @Test
    public void testTaggedPropertyFirst() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("staging"));
        TestConfiguration config = factory.build(provider,
                ".staging.scalar: stagingVal \n" +
                "scalar: testVal"
        );
        assertThat(config).isNotNull();
        assertThat(config.scalar).isEqualTo("stagingVal");
    }


    @Test
    public void testRepeatedTagsInStruct() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("testing"));

        TestConfiguration config = factory.build(provider,
                "struct1: \n" +
                "  val: untagged \n" +
                "  .testing.val: testing1 \n" +
                "\n" +
                "struct2: \n" +
                "  val: untagged \n" +
                "  .testing.val: testing2 \n" +
                "\n" +
                "struct3: \n" +
                "  val: untagged \n" +
                "  .staging.val: testing3 \n" +
                "\n"
        );
        assertThat(config).isNotNull();
        assertThat(config.struct1).isNotNull();
        assertThat(config.struct1.val).isEqualTo("testing1");
        assertThat(config.struct2).isNotNull();
        assertThat(config.struct2.val).isEqualTo("testing2");
        assertThat(config.struct3).isNotNull();
        assertThat(config.struct3.val).isEqualTo("untagged");

    }

    @Ignore
    @Test
    public void testList() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("testing"));

        TestConfiguration config = factory.build(provider,
                "struct: \n" +
                "  val: untagged \n" +
                "  .testing.val : testing \n" +
                "  list: \n" +
                "     - \n" +
                "       val : str1 \n" +
                "       ival: 1 \n" +
                "     - \n" +
                "       val : str2 \n " +
                "       .testing.ival : 102 \n" +
                "       ival : 2 \n" +
                "\n"
        );
        assertThat(config).isNotNull();
        assertThat(config.list).isNotNull();
        assertThat(config.list).hasSize(2);
        assertThat(config.list.get(0).ival).isEqualTo(1);
        assertThat(config.list.get(1).ival).isEqualTo(102);
    }

    public static class TestConfiguration {
        @JsonProperty
        public String scalar;

        @JsonProperty
        public Structure struct1;

        @JsonProperty
        public Structure struct2;

        @JsonProperty
        public Structure struct3;

        @JsonProperty
        public List<Structure> list;

    }

    public static class Structure {
        @JsonProperty
        public String val;
        @JsonProperty
        public Integer ival;
    }

}

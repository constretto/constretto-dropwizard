package org.constretto.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import org.constretto.resolver.ConfigurationContextResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.validation.Validation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author kjeivers
 */
public class ConstrettoConfigurationFactoryTest {

    ConstrettoConfigurationProvider provider;
    ConfigurationFactory<TestConfiguration> factory;
    @Mock
    ConfigurationContextResolver tagResolver;
    @Mock
    ConfigurationSourceProvider source;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        provider = new ConstrettoConfigurationProvider(source);
        factory = new ConstrettoConfigurationFactory<>(
                TestConfiguration.class,
                Validation.buildDefaultValidatorFactory().getValidator(),
                new ObjectMapper(),
                "",
                tagResolver);
    }

    @Test
    public void testNoActiveTagsWithRootScalar() throws IOException, ConfigurationException {
        whenOpenSource(
                "scalar: testVal \n" +
                ".staging.scalar: stagingVal"
        );
        TestConfiguration config = factory.build(provider, "path");
        assertThat(config).isNotNull();
        assertThat(config.scalar).isEqualTo("testVal");
    }

    @Test
    public void testActiveTagWithRootScalar() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("staging"));
        whenOpenSource(
                "scalar: testVal \n" +
                ".staging.scalar: stagingVal");
        TestConfiguration config = factory.build(provider, "path");
        assertThat(config).isNotNull();
        assertThat(config.scalar).isEqualTo("stagingVal");
    }

    @Test
    public void testTaggedPropertyFirst() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("staging"));
        whenOpenSource(
                ".staging.scalar: stagingVal \n" +
                "scalar: testVal");
        TestConfiguration config = factory.build(provider, "path");
        assertThat(config).isNotNull();
        assertThat(config.scalar).isEqualTo("stagingVal");
    }


    @Test
    public void testRepeatedTagsInStruct() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("testing"));
        whenOpenSource(
                "struct1:                 \n" +
                "  val: untagged          \n" +
                "  .testing.val: testing1 \n" +
                "                         \n" +
                "struct2:                 \n" +
                "  val: untagged          \n" +
                "  .testing.val: testing2 \n" +
                "                         \n" +
                "struct3:                 \n" +
                "  val: untagged \n" +
                "  .staging.val: testing3 \n" +
                "\n");
        TestConfiguration config = factory.build(provider, "path");
        assertThat(config).isNotNull();
        assertThat(config.struct1).isNotNull();
        assertThat(config.struct1.val).isEqualTo("testing1");
        assertThat(config.struct2).isNotNull();
        assertThat(config.struct2.val).isEqualTo("testing2");
        assertThat(config.struct3).isNotNull();
        assertThat(config.struct3.val).isEqualTo("untagged");

    }

    @Test
    public void testBasicListOfSequence() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("testing"));
        String yml =
                "list:            \n" +
                "- .staging:      \n" +
                "  val     : str1 \n" +
                "  ival    : 1    \n" +
                "- .testing:      \n" +
                "  val     : str2 \n" +
                "  ival    : 2    \n" +
                "\n";
        whenOpenSource(yml);
        TestConfiguration config = factory.build(provider, "path");
        assertThat(config).isNotNull();
        assertThat(config.list).isNotNull();
        assertThat(config.list).hasSize(1);
        assertThat(config.list.get(0).val).isEqualTo("str2");
        assertThat(config.list.get(0).ival).isEqualTo(2);
    }

    @Test
    public void testListOfSequence() throws IOException, ConfigurationException {
        when(tagResolver.getTags()).thenReturn(Arrays.asList("testing"));
        String yml =
                "list:            \n" +
                "- .staging:      \n" +
                "  val     : str1 \n" +
                "  ival    : 1    \n" +
                "- .testing:      \n" +
                "  val     : str2 \n" +
                "  ival    : 2    \n" +
                "- .testing.val     : str3_testing \n" +
                "  .staging.val     : str3_staging \n" +
                "  ival    : 3    \n" +
                "\n";

        whenOpenSource(yml);
        TestConfiguration config = factory.build(provider, "path");
        assertThat(config).isNotNull();
        assertThat(config.list).isNotNull();
        assertThat(config.list).hasSize(2);
        assertThat(config.list.get(0).val).isEqualTo("str2");
        assertThat(config.list.get(0).ival).isEqualTo(2);
        assertThat(config.list.get(1).val).isEqualTo("str3_testing");
        assertThat(config.list.get(1).ival).isEqualTo(3);
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

    private void whenOpenSource(String str) throws IOException {
        when(source.open(anyString())).thenReturn(new ByteArrayInputStream(str.getBytes()));
    }

}

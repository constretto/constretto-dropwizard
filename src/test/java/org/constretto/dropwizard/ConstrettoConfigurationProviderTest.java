package org.constretto.dropwizard;

import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author kjeivers
 */
public class ConstrettoConfigurationProviderTest {

    ConstrettoConfigurationProvider provider;
    @Mock
    ConfigurationSourceProvider source;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        provider = new ConstrettoConfigurationProvider(source);
    }

    @Test
    public void testNoActiveTagsWithRootScalar() throws IOException, ConfigurationException {
        assertConverted(
                "scalar: testVal\n" +
                        "@staging.scalar: stagingVal",

                "scalar: testVal\n" +
                        ".staging.scalar: stagingVal"
        );
    }

    @Test
    public void testStruct() throws IOException, ConfigurationException {
        assertConverted(
                "struct: \n" +
                        "  val: untagged \n" +
                        "  @testing.val: testing \n" +
                        "\n",

                "struct: \n" +
                        "  val: untagged \n" +
                        "  .testing.val: testing \n" +
                        "\n"
        );
    }

    @Test
    public void testBasicListOfSequence() throws IOException, ConfigurationException {
        assertConverted(
                "list:            \n" +
                        "- @staging:      \n" +
                        "  val     : str1 \n" +
                        "  ival    : 1    \n" +
                        "- @testing:      \n" +
                        "  val     : str2 \n" +
                        "  ival    : 2    \n" +
                        "\n",
                "list:            \n" +
                        "- .staging:      \n" +
                        "  val     : str1 \n" +
                        "  ival    : 1    \n" +
                        "- .testing:      \n" +
                        "  val     : str2 \n" +
                        "  ival    : 2    \n" +
                        "\n"
        );
    }

    @Test
    public void testListOfSequence() throws IOException, ConfigurationException {
        assertConverted(
                "list:            \n" +
                        "- @staging:      \n" +
                        "  val     : str1 \n" +
                        "  ival    : 1    \n" +
                        "- @testing:      \n" +
                        "  val     : str2 \n" +
                        "  ival    : 2    \n" +
                        "-                \n" +
                        "  @testing.val     : str3_t \n" +
                        "  @staging.val     : str3_s \n" +
                        "  ival    : 3    \n" +
                        "\n",
                "list:            \n" +
                        "- .staging:      \n" +
                        "  val     : str1 \n" +
                        "  ival    : 1    \n" +
                        "- .testing:      \n" +
                        "  val     : str2 \n" +
                        "  ival    : 2    \n" +
                        "-                \n" +
                        "  .testing.val     : str3_t \n" +
                        "  .staging.val     : str3_s \n" +
                        "  ival    : 3    \n" +
                        "\n");
    }

    private void assertConverted(String input, String expected) throws IOException {
        InputStream is = provider.open(new StringReader(input));
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String result = "";
        while (br.ready()) {
            String line = br.readLine();
            result += line;
            if (br.ready()) {
                result += "\n";
            }
        }
        assertThat(result.trim()).isEqualTo(expected.trim());
    }

}

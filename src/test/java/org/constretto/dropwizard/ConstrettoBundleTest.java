package org.constretto.dropwizard;

import com.google.common.io.Resources;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.File;
import java.net.URISyntaxException;

import static org.junit.Assert.assertNotNull;

public class ConstrettoBundleTest {

    public static class TestConfiguration extends Configuration {

    }

    @Path("/")
    public static class TestResource {

        @GET
        public String hello() {
            return "hello";
        }
    }


    public static class TestApplication extends Application<TestConfiguration> {

        @Override
        public void initialize(final Bootstrap<TestConfiguration> bootstrap) {
            bootstrap.addBundle(new ConstrettoBundle<>());
        }
        @Override
        public void run(final TestConfiguration testConfiguration, final Environment environment) throws Exception {
            environment.jersey().register(TestResource.class);
        }

    }

    public static String configResource() {
        try {
            return new File(Resources.getResource("test-application.yml").toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @ClassRule
    public static final DropwizardAppRule<TestConfiguration>  APP_RULE = new DropwizardAppRule<>(TestApplication.class, configResource());


    @Test
    public void testApplication() throws Exception {
        assertNotNull(APP_RULE.getConfiguration());

    }
}
package org.constretto.dropwizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.Mark;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.YAMLException;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import javax.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author kjiv
 */
public class ConstrettoConfigurationFactory<T> extends ConfigurationFactory<T> {

    private final ObjectMapper mapper;
    private final YAMLFactory yamlFactory;

    public ConstrettoConfigurationFactory(Class<T> klass, Validator validator, ObjectMapper mapper, String propertyPrefix) {
        super(klass, validator, mapper, propertyPrefix);
        this.mapper = mapper;
        this.yamlFactory = new YAMLFactory();
    }

    @Override
    public T build(ConfigurationSourceProvider provider, String path) throws IOException, ConfigurationException {
        try (InputStream input = provider.open(checkNotNull(path))) {
            final JsonNode node = mapper.readTree(yamlFactory.createParser(input));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            mapper.writeTree(new YAMLFactory().createGenerator(os), node);
            return super.build(new DirectSourceProvider(new ByteArrayInputStream(os.toByteArray())), path);
        } catch (YAMLException e) {
            StringBuilder sb = new StringBuilder(e.getMessage());
            if (e instanceof MarkedYAMLException) {
                Mark mark = ((MarkedYAMLException) e).getProblemMark();
                sb.append(" at line: ").append(mark.getLine() + 1)
                        .append(", column: ").append(mark.getColumn() + 1);
            }
            throw new ConstrettoFilterConfigurationException(path, ImmutableSet.of(sb.toString()), e);
        }
    }


    private static class DirectSourceProvider implements ConfigurationSourceProvider {
        private final InputStream is;

        public DirectSourceProvider(InputStream is) {
            this.is = is;
        }

        @Override
        public InputStream open(String path) throws IOException {
            return is;
        }
    }

    private static class ConstrettoFilterConfigurationException extends ConfigurationException {
        public ConstrettoFilterConfigurationException(String path, Set<String> errors, Throwable cause) {
            super(path, errors, cause);
        }
    }
}

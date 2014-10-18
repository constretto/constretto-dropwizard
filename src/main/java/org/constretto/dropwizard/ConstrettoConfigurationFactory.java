package org.constretto.dropwizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.Mark;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.YAMLException;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import org.constretto.resolver.ConfigurationContextResolver;

import javax.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class is responsible for filtering the constretto-tagged YAML tree
 * based on the set of active constretto tags.
 *
 * @author kjeivers
 */
public class ConstrettoConfigurationFactory<T> extends ConfigurationFactory<T> {

    private final ObjectMapper mapper;
    private final YAMLFactory yamlFactory;
    private final ConfigurationContextResolver tagResolver;

    public ConstrettoConfigurationFactory(Class<T> klass, Validator validator, ObjectMapper mapper,
                                          String propertyPrefix, ConfigurationContextResolver tagResolver) {
        super(klass, validator, mapper, propertyPrefix);
        this.tagResolver = tagResolver;
        this.mapper = mapper;
        this.yamlFactory = new YAMLFactory();
    }

    /**
     * Loads, parses, binds, and validates a configuration object.
     * This implementation filters the YAML tree before delegating to the default ConfigurationFactory
     *
     * @param sourceProvider the provider to to use for reading configuration files
     * @param path     the path of the configuration file
     * @return a validated configuration object that is filtered according to the set of active constretto tags
     * @throws IOException            if there is an error reading the file
     * @throws ConfigurationException if there is an error parsing or validating the file
     */
    @Override
    public T build(ConfigurationSourceProvider sourceProvider, String path) throws IOException, ConfigurationException {
        try (InputStream input = sourceProvider.open(checkNotNull(path))) {
            final JsonNode node = mapper.readTree(yamlFactory.createParser(input));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            mapper.writeTree(new YAMLFactory().createGenerator(os), removeInactiveElements(node, tagResolver.getTags()));
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

    /**
     *
     * @param node The root node to consider
     * @param activeTags The set of active constretto tags
     * @return A modified JsonNode tree filtered according to the activeTags
     */
    private JsonNode removeInactiveElements(JsonNode node, Collection<String> activeTags) {
        switch (node.getNodeType()) {
            case OBJECT:
                return removeInactiveObjectElements((ObjectNode) node, activeTags);
            case ARRAY:
                return removeInactiveArrayElements((ArrayNode) node, activeTags);
            default:
                return node;
        }
    }

    /**
     *
     * @param node An ArrayNode
     * @param activeTags The set of active constretto tags
     * @return A new ArrayNode containing the elements that should be present according to the constretto tags
     */
    private ArrayNode removeInactiveArrayElements(ArrayNode node, Collection<String> activeTags) {
        Iterator<JsonNode> elts = node.elements();
        List<JsonNode> resultElts = new ArrayList<>();
        while (elts.hasNext()) {
            JsonNode child = elts.next();
            if (child.isObject() && child.size() > 0) {
                Map.Entry<String, JsonNode> firstVal = child.fields().next();
                String fieldName = firstVal.getKey();
                // if the first child element consists of a constretto-tag and nothing more: '-.production'
                if (fieldName.startsWith(".") && fieldName.indexOf('.', 1) == -1
                        && "".equals(firstVal.getValue().asText()))
                {
                    String tag = fieldName.substring(1);
                    if (activeTags.contains(tag)) {
                        resultElts.add(removeFirstChild(removeInactiveElements(child, activeTags)));
                    }
                } else {
                    resultElts.add(removeInactiveElements(child, activeTags));
                }
            } else {
                resultElts.add(removeInactiveElements(child, activeTags));
            }
        }
        return node.arrayNode().addAll(resultElts);
    }

    /**
     *
     * @param node An ObjectNode
     * @param activeTags The set of active constretto tags
     * @return A new ObjectNode with only the active child elements
     */
    private JsonNode removeInactiveObjectElements(ObjectNode node, Collection<String> activeTags) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        Map<String, JsonNode> resultFields = new LinkedHashMap<>();
        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            if (fieldName.startsWith(".")) {
                int dot2 = fieldName.indexOf('.', 1);
                if (dot2 != -1) {
                    String tag = fieldName.substring(1, dot2);
                    if (activeTags.contains(tag)) {
                        String resultFieldName = fieldName.substring(dot2 + 1);
                        resultFields.put(resultFieldName, removeInactiveElements(field.getValue(), activeTags));
                    }
                } else if (!resultFields.containsKey(field.getKey())) {
                    resultFields.put(field.getKey(), removeInactiveElements(field.getValue(), activeTags));
                }
            } else if (!resultFields.containsKey(field.getKey())) {
                resultFields.put(field.getKey(), removeInactiveElements(field.getValue(), activeTags));
            }
        }
        return node.objectNode().setAll(resultFields);
    }

    /**
     * Mutates the node by removing first child element
     *
     * @param node The container node to remove elements for
     * @return The same JsonNode that was provided as input, with the first child element removed
     */
    private JsonNode removeFirstChild(JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        if (fields.hasNext()) {
            fields.next();
            fields.remove();
        }
        return node;
    }

    /**
     * A passthrough ConfigurationSourceProvider that wraps an InputStream.
     * This is used when invoking the build() method of the superclass.
     */
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

    /**
     * A concrete subclass of ConfigurationException.
     * The exception class used in super (ConfigurationFactory) is not accessible here.
     * Using ConfigurationValidationException also sounds wrong.
     */
    private static class ConstrettoFilterConfigurationException extends ConfigurationException {

        public ConstrettoFilterConfigurationException(String path, Set<String> errors, Throwable cause) {
            super(path, errors, cause);
        }

    }

}

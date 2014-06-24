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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author kjiv
 */
public class ConstrettoConfigurationFactory<T> extends ConfigurationFactory<T> {

    private final ObjectMapper mapper;
    private final YAMLFactory yamlFactory;
    private final ConfigurationContextResolver tagResolver;

    public ConstrettoConfigurationFactory(Class<T> klass, Validator validator, ObjectMapper mapper, String propertyPrefix, ConfigurationContextResolver tagResolver) {
        super(klass, validator, mapper, propertyPrefix);
        this.tagResolver = tagResolver;
        this.mapper = mapper;
        this.yamlFactory = new YAMLFactory();
    }

    @Override
    public T build(ConfigurationSourceProvider provider, String path) throws IOException, ConfigurationException {
        try (InputStream input = provider.open(checkNotNull(path))) {
            final JsonNode node = mapper.readTree(yamlFactory.createParser(input));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            mapper.writeTree(new YAMLFactory().createGenerator(os), transform(node));
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

    private JsonNode transform(JsonNode node) {
        return transform(node, new HashSet<>(tagResolver.getTags()));
    }

    private JsonNode transform(JsonNode node, Set<String> activeTags) {
        if (node.isObject()) {
            ObjectNode onode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = onode.fields();
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
                            resultFields.put(resultFieldName, transform(field.getValue(), activeTags));
                        }
                    } else if (!resultFields.containsKey(field.getKey())) {
                        resultFields.put(field.getKey(), transform(field.getValue()));
                    }
                } else if (!resultFields.containsKey(field.getKey())) {
                    resultFields.put(field.getKey(), transform(field.getValue(), activeTags));
                }
            }
            ObjectNode resultNode = onode.objectNode();
            resultNode.putAll(resultFields);
            return resultNode;
        } else if (node.isArray()) {
            ArrayNode anode = (ArrayNode) node;
            Iterator<JsonNode> elts = node.elements();
            List<JsonNode> resultElts = new ArrayList<>();
            while (elts.hasNext()) {
                JsonNode child = elts.next();
                if (child.isObject() && child.size() > 0) {
                    Map.Entry<String, JsonNode> firstVal = child.fields().next();
                    String fieldName = firstVal.getKey();
                    if (fieldName.startsWith(".") && fieldName.indexOf('.', 1) == -1 && "".equals(firstVal.getValue().asText())) {
                        String tag = fieldName.substring(1);
                        if (activeTags.contains(tag)) {
                            resultElts.add(removeFirstChild(transform(child, activeTags)));
                        }
                    } else {
                        resultElts.add(transform(child, activeTags));
                    }
                } else {
                    resultElts.add(transform(child, activeTags));
                }
            }
            ArrayNode resultNode = anode.arrayNode();
            resultNode.addAll(resultElts);
            return resultNode;
        } else {
            return node;
        }
    }

    /**
     * Mutates the node by removing first child element
     * @param node The container node to remove elements for
     * @return
     */
    private JsonNode removeFirstChild(JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        if (fields.hasNext()) {
            fields.next();
            fields.remove();
        }
        return node;
    }


}

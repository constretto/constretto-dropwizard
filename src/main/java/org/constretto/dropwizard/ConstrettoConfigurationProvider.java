package org.constretto.dropwizard;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import org.constretto.internal.resolver.DefaultConfigurationContextResolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: kjeivers
 * Date: 15.04.14
 */
public class ConstrettoConfigurationProvider extends FileConfigurationSourceProvider {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private Charset charset = DEFAULT_CHARSET;

    public ConstrettoConfigurationProvider() {
    }

    /**
     *
     * @param path the path to the configuration
     * @return an input stream that is filtered with regard to configuration items marked with constretto tags
     * @throws IOException
     */
    @Override
    public InputStream open(String path) throws IOException {
        Map<PropertyKey, String> lines = readLines(path);
        List<String> tags = getTags();
        List<String> filteredLines = filterLines(lines, tags);
        return toInputStream(filteredLines);
    }

    private List<String> filterLines(Map<PropertyKey, String> lines, List<String> tags) {
        Map<PropertyKey, String> copy = Maps.newLinkedHashMap(lines);
        removeInactiveTaggedLines(copy, tags);
        removeOverriddenLines(copy, tags);
        return toList(copy.values());
    }

    private List<String> toList(Collection<String> prefixedLines) {
        List<String> lines = Lists.newArrayList();
        for (String line : prefixedLines) {
            Optional<String> prefix = PropertyKey.getPrefix(line);
            if (prefix.isPresent()) {
                lines.add(line.substring(prefix.get().length() + 2));
            } else {
                lines.add(line);
            }
        }
        return lines;
    }

    private void removeOverriddenLines(Map<PropertyKey, String> lines, List<String> tags) {
        Iterator<Map.Entry<PropertyKey, String>> iter = lines.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<PropertyKey, String> entry = iter.next();
            if (!entry.getKey().isPrefixed() && existsPrefixed(lines, entry.getKey())) {
                iter.remove();
            }
        }
    }

    private boolean existsPrefixed(Map<PropertyKey, String> lines, PropertyKey toFind) {
        for (PropertyKey key : lines.keySet()) {
            if (key.getKey().equals(toFind.getKey()) && key.isPrefixed()) {
                return true;
            }
        }
        return false;
    }

    private void removeInactiveTaggedLines(Map<PropertyKey, String> lines, List<String> tags) {
        Iterator<Map.Entry<PropertyKey, String>> iter = lines.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<PropertyKey, String> entry = iter.next();
            if (entry.getKey().isPrefixed() && !tags.contains(entry.getKey().getConstrettoPrefix().get())) {
                iter.remove();
            }
        }
    }

    /**
     * @param filteredLines
     * @return an InputStream of the filtered set of lines
     * @throws IOException
     */
    private InputStream toInputStream(List<String> filteredLines) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String line : filteredLines) {
            baos.write(line.getBytes(charset));
            baos.write("\n".getBytes(charset));
            System.out.println(line);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * @return the list of active constretto tags
     */
    private List<String> getTags() {
        return new DefaultConfigurationContextResolver().getTags();
    }

    /**
     * @param path
     * @return an ordered map of the lines in the configuration file
     * @throws IOException
     */
    private Map<PropertyKey, String> readLines(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(super.open(path), charset));
        Map<PropertyKey,String> lines = Maps.newLinkedHashMap();
        int lineNumber = 0;
        while (reader.ready()) {
            String line = reader.readLine();
            lineNumber++;
            String trimmed = line.trim();
            int indexOfEquals = trimmed.indexOf(':');
            String name;
            if (indexOfEquals != -1) {
                name = trimmed.substring(0, indexOfEquals);
            } else {
                name = trimmed;
            }
            lines.put(new PropertyKey(name, lineNumber), line);
        }
        return lines;
    }

    /**
     * Structure that holds the attribute name, constretto tag, etc
     */
    private static class PropertyKey {
        private final Optional<String> constrettoPrefix;
        private final String key;
        private final Integer lineNumber;

        PropertyKey(String name, Integer lineNumber) {
            this.lineNumber = lineNumber;
            constrettoPrefix = getPrefix(name);
            key = constrettoPrefix.isPresent() ? name.substring(constrettoPrefix.get().length() + 2) : name;
        }

        PropertyKey(Optional<String> constrettoPrefix, String key, Integer lineNumber) {
            this.lineNumber = lineNumber;
            this.constrettoPrefix = constrettoPrefix;
            this.key = key;
        }

        public boolean isPrefixed() {
            return constrettoPrefix.isPresent();
        }

        public Optional<String> getConstrettoPrefix() {
            return constrettoPrefix;
        }

        public String getKey() {
            return key;
        }

        static Optional<String> getPrefix(String name) {
            if (name.startsWith("@")) {
                return Optional.of(name.substring(1, name.indexOf('.')));
            } else {
                return Optional.absent();
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("PropertyKey{");
            sb.append("constrettoPrefix=").append(constrettoPrefix);
            sb.append(", key='").append(key).append('\'');
            sb.append(", lineNumber=").append(lineNumber);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PropertyKey that = (PropertyKey) o;

            if (constrettoPrefix != null ? !constrettoPrefix.equals(that.constrettoPrefix) : that.constrettoPrefix != null)
                return false;
            if (key != null ? !key.equals(that.key) : that.key != null) return false;
            if (lineNumber != null ? !lineNumber.equals(that.lineNumber) : that.lineNumber != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = constrettoPrefix != null ? constrettoPrefix.hashCode() : 0;
            result = 31 * result + (key != null ? key.hashCode() : 0);
            result = 31 * result + (lineNumber != null ? lineNumber.hashCode() : 0);
            return result;
        }
    }
}


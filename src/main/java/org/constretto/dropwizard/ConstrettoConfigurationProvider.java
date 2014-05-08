package org.constretto.dropwizard;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author kjeivers@gmail.com
 */
public class ConstrettoConfigurationProvider implements ConfigurationSourceProvider {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private final ConfigurationSourceProvider source;

    private Charset charset = DEFAULT_CHARSET;

    public ConstrettoConfigurationProvider(ConfigurationSourceProvider source) {
        this.source = source;
    }

    /**
     *
     * @param path the path to the configuration
     * @return an input stream that has converted '@tag.' to '.tag.'
     * @throws IOException
     */
    @Override
    public InputStream open(String path) throws IOException {
        return open(new InputStreamReader(source.open(path), charset));
    }

    /**
     * This method is present for easier testability
     */
    InputStream open(Reader reader) throws IOException {
        return toInputStream(FluentIterable
                        .from(readLines(reader))
                        .transform(convertAtSign)
                        .toList()
        );

    }

    private static final Function<String,String> convertAtSign = new Function<String, String>() {
        @Nullable
        @Override
        public String apply(@Nullable String line) {
            if (line != null && line.trim().startsWith("@")) {
                return line.replaceFirst("@", ".");
            } else {
                return line;
            }
        }
    };

    /**
     * @param lines
     * @return an InputStream of the filtered set of lines
     * @throws IOException
     */
    private InputStream toInputStream(List<String> lines) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String line : lines) {
            baos.write(line.getBytes(charset));
            baos.write("\n".getBytes(charset));
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * @param reader for the constretto-annotated configuration source
     * @return an ordered map of the lines in the configuration file
     * @throws IOException
     */
    private List<String> readLines(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        List<String> lines = Lists.newArrayList();
        String line;
        while (br.ready() && (line = br.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

}


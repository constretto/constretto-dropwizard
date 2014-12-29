Dropwizard Constretto Bundle
============================

[![Build Status](https://travis-ci.org/constretto/constretto-dropwizard.svg?branch=master)](https://travis-ci.org/constretto/constretto-dropwizard)

Dropwizard bundle that allows use of constretto tags in yml-configuration like this:

config.yml:

```yaml

database:
  # the JDBC URL
  url: jdbc:oracle:thin:@//oracle-testing:1521/name
  .staging.url: jdbc:oracle:thin:@//oracle-staging:1521/name
  .production.url: jdbc:oracle:thin:@//oracle-production:1521/name

logging:
  appenders:
    - .testing:
      type: console
      threshold: DEBUG
      target: stdout

    - .staging:
      type: console
      threshold: INFO
      target: stdout

```

Usage
-----

```java

import org.constretto.dropwizard.ConstrettoBundle;

public class MyApplication extends Application<Config> {

    @Override
    public void initialize(Bootstrap<Config> configBootstrap) {
        configBootstrap.addBundle(new ConstrettoBundle());
    }
}
```

Maven
-----

This component is distributed through the Sonatype OSS Repository and should thus by widely available

Add the dependency to your pom file:

```xml
    <dependency>
        <groupId>org.constretto</groupId>
        <artifactId>constretto-dropwizard</artifactId>
        <version>1.0</version>
    </dependency>
```

Version 1.0
-------------
 * Release to Sonatype OSS

Version 0.3
----------------
 * Supports tagging of list elements

Version 0.2.1
----------------
 * Updated constretto dependencies to 2.1.4

Version 0.2
----------------
 * Allows tagging with '.env.' (preferred) in addition to '@env.'. The preferred variant makes it valid yaml syntax.
 * Wraps existing ConfigurationSourceProvider to allow chaining of providers
 * Eliminates issue where duplicate property keys in different structs is mixed up
 * Fixes issue where nested values in structs failed to resolve
 * Note: replacing properties in lists is not yet supported

Version 0.1
----------------
 * Supports basic replacement of tagged attributes.
 * Tagging uses the '@' sign (which makes the text illegal yaml syntax)

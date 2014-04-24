Dropwizard Constretto Bundle
============================

Dropwizard bundle that allows use of constretto tags in yml-configuration like this:

config.yml:

```yaml

database:
  # the JDBC URL
  url: jdbc:oracle:thin:@//oracle-testing:1521/name
  @staging.url: jdbc:oracle:thin:@//oracle-staging:1521/name
  @production.url: jdbc:oracle:thin:@//oracle-production:1521/name

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

This component is currently not deployed in any publicly available maven repository (to be done)

    git clone git@github.com:kjeivers/constretto-dropwizard.git
    cd constretto-dropwizard
    maven clean install

Add the dependency to your pom file:

```xml
    <dependency>
        <groupId>org.constretto</groupId>
        <artifactId>constretto-dropwizard</artifactId>
        <version>0.1-SNAPSHOT</version>
    </dependency>
```

# OMAR WMS

## Dockerfile
```
FROM omar-base
EXPOSE 8080
RUN mkdir /usr/share/omar
COPY omar-wms-app-1.0.0-SNAPSHOT.jar /usr/share/omar
RUN chown -R 1001:0 /usr/share/omar
RUN chown 1001:0 /usr/share/omar
RUN chmod -R g+rw /usr/share/omar
RUN find $HOME -type d -exec chmod g+x {} +
USER 1001
WORKDIR /usr/share/omar
CMD java -server -Xms256m -Xmx1024m -Djava.awt.headless=true -XX:+CMSClassUnloadingEnabled -XX:+UseGCOverheadLimit -Djava.security.egd=file:/dev/./urandom -jar omar-wms-app-1.0.0-SNAPSHOT.jar
```
Ref: [omar-base](../../../omar-base/docs/install-guide/omar-ossim-base/)

## JAR
[http://artifacts.radiantbluecloud.com/artifactory/webapp/#/artifacts/browse/tree/General/omar-local/io/ossim/omar/apps/omar-wms-app](http://artifacts.radiantbluecloud.com/artifactory/webapp/#/artifacts/browse/tree/General/omar-local/io/ossim/omar/apps/omar-wms-app)

## Configuration

Additional configuration from [Common Config Settings](../../../omar-common/docs/install-guide/omar-common/#common-config-settings) can be added to the YAML.

```
---
omar:
  wms:
    geoscript:
      url: http://omar-geoscript-app:8080/omar-geoscript/geoscriptApi
    oms:
      chipper:
        url: http://omar-oms-app:8080/omar-oms/chipper
        histOp: auto-minmax
```

where:

* **wms**
 * **geoscript.url** Is the location of the geoscript API for the query service.  This is used to query the layers in the LAYERS parameter.
 * **oms.chipper.url** Is the location of the chipper endpoint for chipping imagery
 * **oms.chipper.histOp**: Is the default histogram operation that is applied if no argument is passed in the styles.  *none, auto-minmax, 
auto-percentile, std-stretch-1, std-stretch-2, or std-stretch-3*
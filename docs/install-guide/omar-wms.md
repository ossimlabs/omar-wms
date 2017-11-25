# OMAR WMS

## Configuration

Additional configuration from [Common Config Settings](../../../omar-common/docs/install-guide/omar-common/#common-config-settings) can be added to the YAML.

* **wms**
 * **geoscript.url** Is the location of the geoscript API for the query service.  This is used to query the layers in the LAYERS parameter.
 * **oms.chipper.url** Is the location of the chipper endpoint for chipping imagery
 * **oms.chipper.histOp**: Is the default histogram operation that is applied if no argument is passed in the styles.  *none, auto-minmax, 
auto-percentile, std-stretch-1, std-stretch-2, or std-stretch-3*

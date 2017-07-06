/**
 * @version 1.1.0
 */
package omar.wms

class OmarWmsUrlMappings
{
  static mappings = {
    "/wms/$action?"( controller: 'wms' )
    "/wms"( controller: 'wms', action: 'index' )
  }
}

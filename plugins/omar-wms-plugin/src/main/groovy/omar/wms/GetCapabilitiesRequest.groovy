/**
 * @author 	sbortman
 * @date  	December 2 2015
 * @version 1.1.0
 */

package omar.wms

import grails.validation.Validateable
import groovy.transform.ToString

/**
 * @brief	Holds the parameters used for the GetCapabilities request to the service
 */
@ToString( includeNames = true )
class GetCapabilitiesRequest implements Validateable
{
  static mapWith = 'none'

  String service /*!< OGC service type */
  String version /*!< Version to request */
  String request /*!< Request type */

  String username

  static mapping = {
    version false
  }

  static constraints = {
    service(nullable: false, blank: false, validator: { val, obj ->
      String result
      if (val != "WMS")
      {
        result = "Invalid service"
        return result
      }
      else
        return true
    })
    version(nullable: false, blank: false, validator: { val, obj ->
      String result
      if (val != "1.1.1" && val != "1.3.0")
      {
        result = "Invalid version"
        return result
      }
      else
        return true
    })
    request(nullable: false, blank: false, validator: { val, obj ->
      String result
      if (val != "GetCapabilities")
      {
        result = "Invalid request"
        return result
      }
      else
        return true
    })
  }
}

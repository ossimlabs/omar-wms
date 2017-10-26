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

  static mapping = {
    version false
  }
}

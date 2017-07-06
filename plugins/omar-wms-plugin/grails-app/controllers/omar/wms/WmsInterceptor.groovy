/**
 * @version 1.1.0
 */

package omar.wms

import omar.core.BindUtil

/**
 * @brief preprocesses parameters of WMS call
 */
class WmsInterceptor
{

  /**
   * @brief constructor of Interceptor sets controler of grails app
   */
  public WmsInterceptor()
  {
    match( controller: 'wms' )
  }

  boolean before()
  {
    /**
     * @brief Switch controls which service is called
     *    Sets up parameters and control to appropriate request
     */
    switch ( actionName?.toUpperCase() )
    {
    case 'GETCAPABILITIES':
      /*!< Case for getcapabilites call sets to GetCapabilitiesRequest container class */
      BindUtil.fixParamNames( GetCapabilitiesRequest, params )
      break
    case 'GETMAP':
      /*!< Case for getmap call sets to GetMapRequest container class */
      BindUtil.fixParamNames( GetMapRequest, params )
      OldMARCompatibility.translate(params) /*!< Translates OldMAR parameter grammar to new O2 parameter grammar */
      break
    }

    true
  }

  boolean after() { true }

  void afterView()
  {
    // no-op
  }
}

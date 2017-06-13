package omar.wms

import grails.validation.Validateable
import groovy.transform.ToString

/**
 * Created by sbortman on 12/2/15.
 */
@ToString( includeNames = true )
class GetMapRequest implements Validateable
{
  static mapWith = 'none'

  String service = "WMS"
  String version = "1.1.1"
  String request = "GetMap"

  Integer width
  Integer height

  String srs  // used for 1.1.1
  String crs  // used for 1.3.0

  String bbox

  String format
  String layers
  String styles

  Boolean transparent

  String filter

  String exceptions
  String bgcolor
  
  static mapping = {
    version false
  }
  static constraints = {
    service(nullable:true, blank:true)
    version(nullable:false, blank:false)
    request(nullable:false, blank:false)

    width(nullable:false, blank:false)
    height(nullable:false, blank:false)

    srs(nullable:true, blank:true, validator: { val, obj -> 
      String result
      if(obj.version == "1.1.1")
      {
        if(!val)
        {
          result = "version is 1.1.1 and the srs field must not be empty"
        }
        else
        {
          def splitValue = val?.split(":")
          if(splitValue.size() != 2)
          {
            result = "srs field must be of the form EPSG:<code>"
          }
          else if(splitValue[0].toUpperCase() != "EPSG")
          {
             result = "srs field must be of the form EPSG:<code>"
          }
        }
      } 
      result
    }) 
    crs(nullable:true, blank:true, validator: { val, obj -> 
      String result
      if(obj.version == "1.3.0")
      {
        if(!val)
        {
          result = "version is 1.3.0 and the crs field must not be empty"
        }
        else
        {
          def splitValue = val?.split(":")
          if(splitValue.size() != 2)
          {
            result = "crs field must be of the form EPSG:code where code is a numeric"
          }
          else if(splitValue[0].toUpperCase() != "EPSG")
          {
             result = "crs field must be of the form EPSG:code where code is a numeric"
          }
        } 
      }
      result
    }) 
    bbox(nullable:false, blank:false, validator: {val, obj ->
      String result

      if(val?.split(",").size() != 4)
      {
        result = "bbox must have 4 values separated by commas"
      }

      result
      })

    format(nullable:true, blank:true)
    layers(nullable:true, blank:true)
    styles(nullable:true, blank:true)

    transparent(nullable:true, blank:true)

    filter(nullable:true, blank:true)

    exceptions(nullable:true, blank:true)
    bgcolor(nullable:true, blank:true)
  }

}

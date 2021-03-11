/**
 * @author sbortman* @date December 2 2015
 * @version 1.1.0
 */

package omar.wms

import grails.validation.Validateable
import groovy.transform.ToString

/**
 * @brief Holds the parameters used for the GetMap request to the service
 */

@ToString(includeNames = true)
class GetMapRequest implements Validateable
{
    static mapWith = 'none'

    String service = "WMS"      /*!< OGC service type */
    String version = "1.1.1"    /*!< Version of WMS to request (1.1.1 by default) */
    String request = "GetMap"   /*!< Request type (must by GetMap) */

    Integer width   /*!< The width of the BBOX in pixels */
    Integer height  /*!< The height of the BBOX in pixels */

    String srs
    /*!< The spatial reference system of the form EPSG:code where code is a spatial reference code for and required by WMS verison 1.1.1 */
    String crs
    /*!< The spatial reference system of the form EPSG:code where code is a spatial reference code for and required by WMS verison 1.3.0 */

    String bbox /*!< The bounding box in the units of the srs or crs code. Of the form minx,miny,maxx,maxy */

    String format /*!< MIME Type of result image */
    String layers /*!< Type name */
    String styles /*!< Styling applied to the result image */

    Boolean transparent /*!< Defines the transparency */

    String filter /*!< Optional field to hold the where clause of the table we are querying */

    String exceptions
    /*!< Defines the types of exceptions to be returned. Can be of the values application/vnd.ogc.se_xml, application/vnd.ogc.se_inimage, or application/vnd.ogc.se_blank */
    String bgcolor

    String username

    static mapping = {
        version false
    }

    /*!< Defines proper grammar for the parameter values for an acceptable call. */
    static constraints = {
        service(nullable: true, blank: true)
        version(nullable: false, blank: false)
        request(nullable: false, blank: false)

        width(nullable: false, blank: false)
        height(nullable: false, blank: false)

        srs(nullable: true, blank: true, validator: { val, obj ->
            String result
            if (obj.version == "1.1.1")
            {
                if (val)
                {
                    def splitValue = val?.split(":")
                    if (splitValue.size() != 2)
                    {
                        result = "srs field must be of the form EPSG:<code>"
                    }
                    else if (splitValue[0].toUpperCase() != "EPSG")
                    {
                        result = "srs field must be of the form EPSG:<code>"
                    }
                }
                else
                {
                    result = "version is 1.1.1 and the srs field must not be empty"
                }
            }
            result
        })
        crs(nullable: true, blank: true, validator: { val, obj ->
            String result
            if (obj.version == "1.3.0")
            {
                if (val)
                {
                    def splitValue = val?.split(":")
                    if (splitValue.size() != 2)
                    {
                        result = "crs field must be of the form EPSG:code where code is a numeric"
                    }
                    else if (splitValue[0].toUpperCase() != "EPSG")
                    {
                        result = "crs field must be of the form EPSG:code where code is a numeric"
                    }
                }
                else
                {
                    result = "version is 1.3.0 and the crs field must not be empty"
                }
            }
            result
        })
        bbox(nullable: false, blank: false, validator: { val, obj ->
            String result

            if (val?.split(",").size() != 4)
            {
                result = "bbox must have 4 values separated by commas"
            }

            result
        })

        format(nullable: true, blank: true)
        layers(nullable: true, blank: true)
        styles(nullable: true, blank: true)

        transparent(nullable: true, blank: true)

        filter(nullable: true, blank: true)

        exceptions(nullable: true, blank: true)
        bgcolor(nullable: true, blank: true)
    }
}


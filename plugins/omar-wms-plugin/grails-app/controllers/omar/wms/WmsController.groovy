/**
 * @version 1.1.0
 */
package omar.wms

import io.swagger.annotations.*

import omar.core.BindUtil
import omar.core.HttpStatus
import omar.core.OgcExceptionUtil
import omar.core.OmarWebUtils
import groovy.util.logging.Slf4j
import org.grails.web.util.WebUtils

import java.nio.charset.StandardCharsets


/**
 * @brief Grails controller
 *
 *		Control flow of request to the WMS service
 */
@Api( value = "/wms",
		description = "WMS Support"
)
@Slf4j
class WmsController
{
	def webMappingService

	def index()
	{
		def op = params.find { it.key.equalsIgnoreCase( 'request' ) }?.value


		switch ( op?.toUpperCase() )
		{
			case "GETCAPABILITIES":
				getCapabilities()
				break
			case "GETMAP":
				getMap()
				break
			case "GETPSM":
				getPsm()
				break
			case "GETSTYLES":
				getStyles()
				break
			case "GETLEGENDGRAPHIC":
				getLegendGraphic()
				break
		}
	}

	/**
	 * 		Returns the capabilites of the given WMS version
	 *
	 * @param  wmsParams parameters to the WMS service request GetCapabilities
	 */
	@ApiOperation( value = "Get the capabilities of the server",
			produces = 'application/vnd.ogc.wms_xml',
			httpMethod = "GET",
			nickname = "getCapabilities")
	@ApiImplicitParams( [
			@ApiImplicitParam( name = 'service', value = 'OGC Service type', allowableValues = "WMS", defaultValue = 'WMS', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'version', value = 'Version to request', allowableValues = "1.1.1,1.3.0", defaultValue = '1.3.0', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'request', value = 'Request type', allowableValues = "GetCapabilities", defaultValue = 'GetCapabilities', paramType = 'query', dataType = 'string', required = true ),
	] )
	def getCapabilities()
	{
		GetCapabilitiesRequest wmsParams = new GetCapabilitiesRequest()
		BindUtil.fixParamNames( GetCapabilitiesRequest, params )

		bindData( wmsParams, params )
		wmsParams.username = webMappingService.extractUsernameFromRequest(request)

		OutputStream outputStream = null
		try
		{
			outputStream = response.outputStream
			if(wmsParams.validate())
			{
				Map results = webMappingService.getCapabilities( wmsParams )
				if(results.status) response.status = results.status
				if(results.contentType) response.contentType = results.contentType
				if(results.buffer?.length) response.contentLength = results.buffer.length
				if(outputStream)
				{
					outputStream << results.buffer
				}
			}
			else
			{
				response.status = HttpStatus.BAD_REQUEST

				HashMap ogcExceptionResult = OgcExceptionUtil.formatWmsException(wmsParams)
				response.contentType = ogcExceptionResult.contentType
				response.contentLength = ogcExceptionResult.buffer.length
				outputStream << ogcExceptionResult.buffer
			}
		}
		catch ( IOException e )
		{
			log.error("Error writing response output stream", e)
		}
		finally
		{
			outputStream?.close()
		}

		return outputStream
	}

	/**
	 * 		Returns the images of the given WMS getMap request
	 */
	@ApiOperation( value = "Get image from the server",
			produces = 'application/xml,application/json',
			httpMethod="GET",
			notes = """
* **version** can be 1.1.1 or 1.3.0. If 1.3.0 is used **crs**
   must be specified and if version is 1.1.1 is used then **srs**
   field is used
* **request** must be GetMap
* **layers** can be either of the form omar:raster_entry or
   omar:raster_entry.id where **id** is the record ID in the database
* **filter** can contain the where clause of the table we are
   querying.
* **srs** is the spatial reference system of the form EPSG:code
   where **code** is a spatial reference code such as 4326 or 3857,
   .. etc
* **crs** is the spatial reference system of the form EPSG:code
   where **code** is a spatial reference code such as 4326 or 3857,
   .. etc
* **bbox** cut box in the units of the srs or crs code. Is comma
   separated values of the form minx,miny,maxx,maxy
* **width** defines the pixel width of the **bbox** cut.
* **height** defines the pixel height of the **bbox** cut
* **exceptions** defines the type of excpetions you can return.
   values can be one of application/vnd.ogc.se_xml,application/vnd.ogc.se_inimage,application/vnd.ogc.se_blank
* **styles** Is a JSON formated string. That allows one to have
   added control over the pixel return of the image(s). Here is an
   example call:
```
   {
      "bands": "1,2,3",
      "histOp": "auto-percentile",
      "sharpenMode" : "none",
      "contrast" : "1.0",
      "brightness" : "0.0",
      "resamplerFilter": "bilinear",
      "histCenterTile": "false"
   }
```
where:
* **bands:** is a one based band selection list. First band starts at 1.
* **histOp:** values supported none, auto-minmax,
   auto-percentile, std-stretch-1, std-stretch-2, or std-stretch-3
* **sharpenMode:** values supported none, light, or heavy.
* **contrast:** Allows one to control the contrast of an
   image. This is a multiplier.
* **brightness:** Allows one to control the brightness of the image.
    This is expressed as a normalized value between -1 and 1.
    You can go higher values but just know it's a normalized and not absolute values.
* **resamplerFilter:** values supported nearest-neighbor, bilinear,
    cubic, gaussian, blackman, bspline, hanning, hamming, hermite, mitchell, quadratic,
    sinc, magic
* **histCenterTile:**Currently calculates the histogram from center of image. Can be true|false
    """
	)
	@ApiImplicitParams( [
			@ApiImplicitParam( name = 'service', value = 'OGC service type', allowableValues = "WMS", defaultValue = 'WMS', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'version', value = 'Version to request', allowableValues = "1.1.1, 1.3.0", defaultValue = '1.3.0', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'request', value = 'Request type', allowableValues = "GetMap", defaultValue = 'GetMap', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'layers', value = 'Type name', defaultValue = "omar:raster_entry", paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'filter', value = 'Filter', paramType = 'query', dataType = 'string', required = false ),
			@ApiImplicitParam( name = 'srs', value = 'Spatial Reference System (Version 1.1.1)', defaultValue = "epsg:4326", paramType = 'query', dataType = 'string', required = false ),
			@ApiImplicitParam( name = 'crs', value = 'Spatial Reference System (Version 1.3.0)', defaultValue = "epsg:4326", paramType = 'query', dataType = 'string', required = false ),
			@ApiImplicitParam( name = 'bbox', value = 'Bounding box', defaultValue = "-180,-90,180,90", paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'width', value = 'Width of result image', defaultValue = "1024", paramType = 'query', dataType = 'integer', required = true ),
			@ApiImplicitParam( name = 'height', value = 'Height of result image', defaultValue = "512", paramType = 'query', dataType = 'integer', required = true ),
			@ApiImplicitParam( name = 'format', value = 'MIME Type of result image', defaultValue = "image/vnd.jpeg-png", allowableValues = "image/jpeg, image/png, image/gif, image/vnd.jpeg-png", paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'transparent', value = 'Defines the transparency', defaultValue = "FALSE", allowableValues = "TRUE,FALSE", paramType = 'query', dataType = 'boolean', required = false ),
			@ApiImplicitParam( name = 'styles', value = 'Styles to apply to image ', defaultValue = "", paramType = 'query', dataType = 'string', required = false ),
			@ApiImplicitParam( name = 'exceptions', value = 'Valid exceptions', defaultValue = "", allowableValues = "application/vnd.ogc.se_xml,application/vnd.ogc.se_inimage,application/vnd.ogc.se_blank", paramType = 'query', dataType = 'string', required = false ),
	] )
	def getMap( )
	{
		getMapOrPsm(false)
	}

	/**
	 * 		Returns the pan-sharpened images of the given WMS getPsm request
	 */




	// the following is commented out to remove from the Swagger UI in order to
	// avoid cypress testing

//	@ApiOperation( value = "Get a pan-sharpened image provided _____ parameters",
//			produces = 'application/xml,application/json',
//			httpMethod="GET",
//			notes = """
//* **version** can be 1.1.1 or 1.3.0. If 1.3.0 is used **crs**
//   must be specified and if version is 1.1.1 is used then **srs**
//   field is used
//* **request** must be GetMap
//* **layers** can be either of the form omar:raster_entry or
//   omar:raster_entry.id where **id** is the record ID in the database
//* **filter** can contain the where clause of the table we are
//   querying.
//* **srs** is the spatial reference system of the form EPSG:code
//   where **code** is a spatial reference code such as 4326 or 3857,
//   .. etc
//* **crs** is the spatial reference system of the form EPSG:code
//   where **code** is a spatial reference code such as 4326 or 3857,
//   .. etc
//* **bbox** cut box in the units of the srs or crs code. Is comma
//   separated values of the form minx,miny,maxx,maxy
//* **width** defines the pixel width of the **bbox** cut.
//* **height** defines the pixel height of the **bbox** cut
//* **exceptions** defines the type of excpetions you can return.
//   values can be one of application/vnd.ogc.se_xml,application/vnd.ogc.se_inimage,application/vnd.ogc.se_blank
//* **styles** Is a JSON formated string. That allows one to have
//   added control over the pixel return of the image(s). Here is an
//   example call:
//```
//   {
//      "bands": "1,2,3",
//      "histOp": "auto-percentile",
//      "sharpenMode" : "none",
//      "contrast" : "1.0",
//      "brightness" : "0.0",
//      "resamplerFilter": "bilinear",
//      "histCenterTile": "false"
//   }
//```
//where:
//* **bands:** is a one based band selection list. First band starts at 1.
//* **histOp:** values supported none, auto-minmax,
//   auto-percentile, std-stretch-1, std-stretch-2, or std-stretch-3
//* **sharpenMode:** values supported none, light, or heavy.
//* **contrast:** Allows one to control the contrast of an
//   image. This is a multiplier.
//* **brightness:** Allows one to control the brightness of the image.
//    This is expressed as a normalized value between -1 and 1.
//    You can go higher values but just know it's a normalized and not absolute values.
//* **resamplerFilter:** values supported nearest-neighbor, bilinear,
//    cubic, gaussian, blackman, bspline, hanning, hamming, hermite, mitchell, quadratic,
//    sinc, magic
//* **histCenterTile:**Currently calculates the histogram from center of image. Can be true|false
//    """
//	)
//	@ApiImplicitParams( [
//			@ApiImplicitParam( name = 'service', value = 'OGC service type', allowableValues = "WMS", defaultValue = 'WMS', paramType = 'query', dataType = 'string', required = true ),
//			@ApiImplicitParam( name = 'version', value = 'Version to request', allowableValues = "1.1.1, 1.3.0", defaultValue = '1.3.0', paramType = 'query', dataType = 'string', required = true ),
//			@ApiImplicitParam( name = 'request', value = 'Request type', allowableValues = "GetPsm", defaultValue = 'GetPsm', paramType = 'query', dataType = 'string', required = true ),
//			@ApiImplicitParam( name = 'layers', value = 'Type name', defaultValue = "omar:raster_entry", paramType = 'query', dataType = 'string', required = true ),
//			@ApiImplicitParam( name = 'filter', value = 'Filter', paramType = 'query', dataType = 'string', required = false ),
//			@ApiImplicitParam( name = 'srs', value = 'Spatial Reference System (Version 1.1.1)', defaultValue = "epsg:4326", paramType = 'query', dataType = 'string', required = false ),
//			@ApiImplicitParam( name = 'crs', value = 'Spatial Reference System (Version 1.3.0)', defaultValue = "epsg:4326", paramType = 'query', dataType = 'string', required = false ),
//			@ApiImplicitParam( name = 'bbox', value = 'Bounding box', defaultValue = "-180,-90,180,90", paramType = 'query', dataType = 'string', required = true ),
//			@ApiImplicitParam( name = 'width', value = 'Width of result image', defaultValue = "1024", paramType = 'query', dataType = 'integer', required = true ),
//			@ApiImplicitParam( name = 'height', value = 'Height of result image', defaultValue = "512", paramType = 'query', dataType = 'integer', required = true ),
//			@ApiImplicitParam( name = 'format', value = 'MIME Type of result image', defaultValue = "image/vnd.jpeg-png", allowableValues = "image/jpeg, image/png, image/gif, image/vnd.jpeg-png", paramType = 'query', dataType = 'string', required = true ),
//			@ApiImplicitParam( name = 'transparent', value = 'Defines the transparency', defaultValue = "FALSE", allowableValues = "TRUE,FALSE", paramType = 'query', dataType = 'boolean', required = false ),
//			@ApiImplicitParam( name = 'styles', value = 'Styles to apply to image ', defaultValue = "", paramType = 'query', dataType = 'string', required = false ),
//			@ApiImplicitParam( name = 'exceptions', value = 'Valid exceptions', defaultValue = "", allowableValues = "application/vnd.ogc.se_xml,application/vnd.ogc.se_inimage,application/vnd.ogc.se_blank", paramType = 'query', dataType = 'string', required = false ),
//	] )
	def getPsm( )
	{
		getMapOrPsm(true)
	}

	def getMapOrPsm (Boolean isPsm){
		GetMapRequest wmsParams =  new GetMapRequest()
		bindData(wmsParams, BindUtil.fixParamNames( GetMapRequest, params ))
		wmsParams.username = webMappingService.extractUsernameFromRequest(request)

		OutputStream outputStream = null
		try
		{
			outputStream = response.outputStream
			if(wmsParams.validate())
			{
				Map results = webMappingService.getMap( wmsParams, isPsm )
				if(results.status) response.status = results.status
				if(results.contentType) response.contentType = results.contentType
				if(results.buffer?.length) response.contentLength = results.buffer.length
				if(outputStream)
				{
					outputStream << results.buffer
				}
			}
			else
			{
				response.status = HttpStatus.BAD_REQUEST

				HashMap ogcExceptionResult = OgcExceptionUtil.formatWmsException(wmsParams)
				response.contentType = ogcExceptionResult.contentType
				response.contentLength = ogcExceptionResult.buffer.length
				outputStream << ogcExceptionResult.buffer
			}
		}
		catch ( IOException e )
		{
			log.error("Error writing response output stream", e)
		}
		finally
		{
			outputStream?.close()
		}

		return outputStream
	}

	// Keep as endpoint
	def getStyles()
	{
		render webMappingService.getStyles(params)
	}

	// Keep as endpoint
	def getLegendGraphic()
	{
		render webMappingService.getLegendGraphic(params)
	}

	/**
	 * Encodes the response to gzip if requested
	 * @param inputText The original, unencoded response
	 * @return The encoded response
	 */
	private encodeResponse(String inputText) {
		def outputText
		String acceptEncoding = WebUtils.retrieveGrailsWebRequest().getCurrentRequest().getHeader('accept-encoding')

		if ( acceptEncoding?.contains( OmarWebUtils.GZIP_ENCODE_HEADER_PARAM ) ) {
			response.setHeader 'Content-Encoding', OmarWebUtils.GZIP_ENCODE_HEADER_PARAM
			outputText = OmarWebUtils.gzippify( inputText, StandardCharsets.UTF_8.name() )
		} else {
			outputText = inputText
		}
		return outputText
	}
}
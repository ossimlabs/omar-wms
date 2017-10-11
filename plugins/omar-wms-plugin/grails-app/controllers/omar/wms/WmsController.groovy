/**
 * @version 1.1.0
 */
package omar.wms

import io.swagger.annotations.*

import omar.core.BindUtil
import omar.core.IpUtil
import omar.core.HttpStatus
import omar.core.OgcExceptionUtil


/**
 * @brief Grails controller
 *
 *		Control flow of request to the WMS service
 */
@Api( value = "/wms",
		description = "WMS Support"
)
class WmsController
{
	def webMappingService
//	def springSecurityService
	def wmsLogService

	def index()
	{
		def wmsParams = params - params.subMap( [ 'controller', 'format' ] )
		def op = wmsParams.find { it.key.equalsIgnoreCase( 'request' ) }

		switch ( op?.value?.toUpperCase() )
		{
		case "GETCAPABILITIES":
			forward action: 'getCapabilities'
			break
		case "GETMAP":
			forward action: 'getMap'
			break
		}
	}

	/**
	 * 		Returns the capabilites of the given WMS version
	 *
	 * @param  wmsParams parameters to the WMS service request GetCapabilities
	 */
	@ApiOperation( value = "Get the capabilities of the server", produces = 'application/vnd.ogc.wms_xml' )
	@ApiImplicitParams( [
			@ApiImplicitParam( name = 'service', value = 'OGC Service type', allowableValues = "[WMS]", defaultValue = 'WMS', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'version', value = 'Version to request', allowableValues = "[1.1.1, 1.3.0]", defaultValue = '1.3.0', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'request', value = 'Request type', allowableValues = "[GetCapabilities]", defaultValue = 'GetCapabilities', paramType = 'query', dataType = 'string', required = true ),
	] )
	def getCapabilities( GetCapabilitiesRequest wmsParams )
	{
		BindUtil.fixParamNames( GetCapabilitiesRequest, params )
		bindData( wmsParams, params )

		def results = webMappingService.getCapabilities( wmsParams )

		render contentType: results.contentType, text: results.buffer

	}

	/**
	 * 		Returns the images of the given WMS getMap request
	 */
	@ApiOperation( value = "Get image from the server",
		produces = 'application/xml,application/json',
		notes = """
<style>
    tab { padding-left: 2em; }
</style>
    <ul>
        <li>
            <b>version</b> can be 1.1.1 or 1.3.0.  If 1.3.0 is used <b>crs</b> must be specified and if version is 1.1.1 is used then <b>srs</b> field is used<br/>
            <b>request</b> must be GetMap<br/>
            <b>layers</b> can be either of the form omar:raster_entry or omar:raster_entry.id where <b>id</b> is the record ID in the database<p/>
            <b>filter</b> can contain the where clause of the table we are querying.<p/>
            <b>srs</b> is the spatial reference system of the form EPSG:code where <b>code</b> is a spatial reference code such as 4326 or 3857, .. etc<p/>
            <b>crs</b> is the spatial reference system of the form EPSG:code where <b>code</b> is a spatial reference code such as 4326 or 3857, .. etc<p/>
            <b>bbox</b> cut box in the units of the srs or crs code.  Is comma separated values of the form minx,miny,maxx,maxy <p/>
            <b>width</b> defines the pixel width of the <b>bbox</b> cut.<p/>
            <b>height</b> defines the pixel height of the <b>bbox</b> cut<p/>
            <b>exceptions</b> defines the type of excpetions you can return. values can be one of application/vnd.ogc.se_xml,application/vnd.ogc.se_inimage,application/vnd.ogc.se_blank<p/>
            <b>styles</b><p/>
            Is a JSON formated string. That allows one to have added control over the pixel return of the image(s). Here is an example call:
            <pre>
     {"bands": "1,2,3",
     "histOp": "auto-percentile",
     "sharpenMode" : "none",
     "contrast" : "1.0",
     "brightness" : "0.0",
     "resamplerFilter": "bilinear",
     "histCenterTile": "false"
     }
            </pre>
            where <br/>
            <ul>
	            <tab><b>bands:</b> is a one based band selection list.  First band starts at 1.</tab><br/>
	            <tab><b>histOp:</b> values supported none, auto-minmax, </tab><br/>
	            <tab>auto-percentile, std-stretch-1, std-stretch-2, or std-stretch-3</tab><br/>
	            <tab><b>sharpenMode:</b> values supported  none, light, or heavy.</tab><br/>
	            <tab><b>contrast:</b> Allows one to control the contrast of an<br/>
	            <tab>image. This is a multiplier.</tab><br/>
	            <tab><b>brightness:</b> Allows one to control the brightness of the image.<br/>
	            <tab>This is expressed as a normalized value between -1 and 1.<br/>
	            <tab>You can go higher values but just know it's a normalized and not absolute values.</tab><br/>
	            <tab><b>resamplerFilter:</b>values supported nearest-neighbor, bilinear, </br>
	            <tab>cubic, gaussian, blackman, bspline, hanning, hamming, hermite, mitchell, quadratic,<br/>
	            <tab>sinc, magic</tab><br/>
               <tab><b>histCenterTile:</b>Currently calculates the histogram from center of image.  Can be true|false<br/>
            </ul>
        </li>
        <br>
     </ul>
		"""
		 )
	@ApiImplicitParams( [
			@ApiImplicitParam( name = 'service', value = 'OGC service type', allowableValues = "[WMS]", defaultValue = 'WMS', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'version', value = 'Version to request', allowableValues = "[1.1.1, 1.3.0]", defaultValue = '1.3.0', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'request', value = 'Request type', allowableValues = "[GetMap]", defaultValue = 'GetMap', paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'layers', value = 'Type name', defaultValue = "omar:raster_entry", paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'filter', value = 'Filter', paramType = 'query', dataType = 'string', required = false ),
			@ApiImplicitParam( name = 'srs', value = 'Spatial Reference System (Version 1.1.1)', defaultValue = "epsg:4326", paramType = 'query', dataType = 'string', required = false ),
			@ApiImplicitParam( name = 'crs', value = 'Spatial Reference System (Version 1.3.0)', defaultValue = "epsg:4326", paramType = 'query', dataType = 'string', required = false ),
			@ApiImplicitParam( name = 'bbox', value = 'Bounding box', defaultValue = "-180,-90,180,90", paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'width', value = 'Width of result image', defaultValue = "1024", paramType = 'query', dataType = 'int', required = true ),
			@ApiImplicitParam( name = 'height', value = 'Height of result image', defaultValue = "512", paramType = 'query', dataType = 'int', required = true ),
			@ApiImplicitParam( name = 'format', value = 'MIME Type of result image', defaultValue = "image/jpeg", allowableValues = "[image/jpeg, image/png, image/gif]", paramType = 'query', dataType = 'string', required = true ),
			@ApiImplicitParam( name = 'transparent', value = 'Defines the transparency', defaultValue = "FALSE", allowableValues = "[TRUE,FALSE]", paramType = 'query', dataType = 'boolean', required = false ),
			@ApiImplicitParam( name = 'styles', value = 'Styles to apply to image ', defaultValue = "", paramType = 'query', dataType = 'string', required = false ),
			@ApiImplicitParam( name = 'exceptions', value = 'Valid exceptions', defaultValue = "", allowableValues = "[application/vnd.ogc.se_xml,application/vnd.ogc.se_inimage,application/vnd.ogc.se_blank]", paramType = 'query', dataType = 'string', required = false ),
	] )
	def getMap( )
	{
		GetMapRequest wmsParams =  new GetMapRequest()

      bindData(wmsParams, BindUtil.fixParamNames( GetMapRequest, params ))

		def outputStream = null
		try
		{
   		outputStream = response.outputStream
			if(wmsParams.validate())
			{
				def result = webMappingService.getMap( wmsParams )
				if(result.status) response.status = result.status
				if(result.contentType) response.contentType = result.contentType
				if(result.buffer?.length) response.contentLength = result.buffer.length
				if(outputStream)
				{
					outputStream << result.buffer
				}

				def otherParams = result.metrics

				otherParams.ip = IpUtil.getClientIpAddr(request)
				otherParams.endDate = new Date()
				otherParams.endTime = System.currentTimeMillis()
//				wmsLogService.logGetMapRequest( wmsParams, otherParams )
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
		catch ( e )
		{
			log.error(e.toString())
		}
		finally{
			if(outputStream!=null)
			{
				try{
					outputStream.close()
				}
				catch(e)
				{
					log.error(e.toString())
				}
			}
		}
	}
}

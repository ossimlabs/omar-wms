/**
 * @version 1.1.0
 */
package omar.wms

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.xml.StreamingMarkupBuilder
import omar.core.DateUtil
import omar.core.HttpStatus
import omar.core.OgcExceptionUtil
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.PrecisionModel

import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import org.springframework.util.FastByteArrayOutputStream


class WebMappingService implements InitializingBean
{
  static transactional = false

  static final IMAGE_SPACE_PROJECTION_ID =  "EPSG:99999"

  def grailsLinkGenerator
  def grailsApplication
  def geoscriptService
  def footprintService
  def stagerService

  def serverData
  def projections

  @Value ('${omar.wms.oms.chipper.histOp}')
  String autoHistogramMode


  @Value ('${omar.wms.oms.chipper.url}')
  String omsChipperUrl

  @Override
  void afterPropertiesSet() throws Exception
  {
    serverData = grailsApplication.config?.geoscript?.serverData
    projections = geoscriptService.listProjections()

  }

  enum RenderMode {
    BLANK, GEOSCRIPT, FILTER
  }

  def getCapabilities(GetCapabilitiesRequest wmsParams)
  {
    def contentType, buffer
    def version = wmsParams?.version ?: "1.3.0"
    def schemaLocation = grailsLinkGenerator.link( absolute: true, uri: "/schemas/wms/1.3.0/capabilities_1_3_0.xsd" )
    def docTypeLocation = grailsLinkGenerator.link( absolute: true, uri: "/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd" )
    def model = geoscriptService.capabilitiesData

    def requestType = "GET"
    def requestMethod = "GetCapabilities"
    Date startTime = new Date()
    def responseTime
    def requestInfoLog

    def x = {
      mkp.xmlDeclaration()

      if ( version == "1.1.1" )
      {
        mkp.yieldUnescaped """<!DOCTYPE WMT_MS_Capabilities SYSTEM "${docTypeLocation}">"""
      }

      def rootTag = (version == "1.1.1") ? "WMT_MS_Capabilities" : "WMS_Capabilities"
      def rootAttributes = [version: version]

      mkp.declareNamespace(
              xlink: "http://www.w3.org/1999/xlink",
      )

      if ( version == "1.3.0" )
      {
        mkp.declareNamespace(
                xsi: "http://www.w3.org/2001/XMLSchema-instance"
        )

        rootAttributes['xmlns'] = "http://www.opengis.net/wms"
        rootAttributes['xsi:schemaLocation'] = "http://www.opengis.net/wms ${schemaLocation}"
      }

      "${rootTag}"( rootAttributes ) {

        Service {
          Name( serverData.Service.Name )
          Title( serverData.Service.Title )
          Abstract( serverData.Service.Abstract )
          KeywordList {
            serverData.Service.KeywordList.each { keyword ->
              Keyword( keyword )
            }
          }
          OnlineResource( 'xlink:type': "simple", 'xlink:href': serverData.Service.OnlineResource )
          ContactInformation {
            ContactPersonPrimary {
              ContactPerson( serverData.Service.ContactInformation.ContactPersonPrimary.ContactPerson )
              ContactOrganization( serverData.Service.ContactInformation.ContactPersonPrimary.ContactOrganization )
            }
            ContactPosition( serverData.Service.ContactInformation.ContactPosition )
            ContactAddress {
              AddressType( serverData.Service.ContactInformation.ContactAddress.AddressType )
              Address( serverData.Service.ContactInformation.ContactAddress.Address )
              City( serverData.Service.ContactInformation.ContactAddress.City )
              StateOrProvince( serverData.Service.ContactInformation.ContactAddress.StateOrProvince )
              PostCode( serverData.Service.ContactInformation.ContactAddress.PostCode )
              Country( serverData.Service.ContactInformation.ContactAddress.Country )
            }
            ContactVoiceTelephone( serverData.Service.ContactInformation.ContactVoiceTelephone )
            ContactFacsimileTelephone( serverData.Service.ContactInformation.ContactFacsimileTelephone )
            ContactElectronicMailAddress( serverData.Service.ContactInformation.ContactElectronicMailAddress )
          }
          Fees( serverData.Service.Fees )
          AccessConstraints( serverData.Service.AccessConstraints )
        }
        Capability {
          Request {
            GetCapabilities {
              contentType = (version == '1.1.1') ? "application/vnd.ogc.wms_xml" : "text/xml"
              Format( contentType )
              DCPType {
                HTTP {
                  Get {
                    OnlineResource( 'xlink:type': "simple",
                            'xlink:href': grailsLinkGenerator.link( absolute: true, controller: 'wms', action: 'getCapabilities' ) )
                  }
                  Post {
                    OnlineResource( 'xlink:type': "simple",
                            'xlink:href': grailsLinkGenerator.link( absolute: true, controller: 'wms', action: 'getCapabilities' ) )
                  }
                }
              }
            }
            GetMap {
              serverData.Capability.Request.GetMap.Format.each { format ->
                Format( format )
              }
              DCPType {
                HTTP {
                  Get {
                    OnlineResource( 'xlink:type': "simple",
                            'xlink:href': grailsLinkGenerator.link( absolute: true, controller: 'wms', action: 'getMap' ) )
                  }
                }
              }
            }
          }
          Exception {
            serverData.Capability.Exception.Format.each { format ->
              Format( format )
            }
          }
          Layer {
            Title( serverData.Capability.Layer.Title )
            Abstract( serverData.Capability.Layer.Abstract )
            def crsTag = (version == '1.1.1') ? "SRS" : "CRS"
            projections?.each { crs ->
              "${crsTag}"( crs?.id )
            }
            if ( version == '1.3.0' )
            {
              EX_GeographicBoundingBox {
                westBoundLongitude( serverData.Capability.Layer.BoundingBox.minLon )
                eastBoundLongitude( serverData.Capability.Layer.BoundingBox.maxLon )
                southBoundLatitude( serverData.Capability.Layer.BoundingBox.minLat )
                northBoundLatitude( serverData.Capability.Layer.BoundingBox.maxLat )
              }
              BoundingBox( CRS: serverData.Capability.Layer.BoundingBox.crs,
                      minx: serverData.Capability.Layer.BoundingBox.minLon,
                      miny: serverData.Capability.Layer.BoundingBox.minLat,
                      maxx: serverData.Capability.Layer.BoundingBox.maxLon,
                      maxy: serverData.Capability.Layer.BoundingBox.maxLat
              )
            }
            else
            {
              LatLonBoundingBox(
                      minx: serverData.Capability.Layer.BoundingBox.minLon,
                      miny: serverData.Capability.Layer.BoundingBox.minLat,
                      maxx: serverData.Capability.Layer.BoundingBox.maxLon,
                      maxy: serverData.Capability.Layer.BoundingBox.maxLat
              )
            }
            model?.featureTypes?.each { featureType ->
              Layer( queryable: "1", opaque: "0" ) {
                Name( "${featureType.namespace.prefix}:${featureType.name}" )
                Title( featureType.title )
                Abstract( featureType.description )
                Keywords {
                  featureType.keywords.each { keyword ->
                    Keyword( keyword )
                  }
                }
                def bounds = featureType.geoBounds

                "${crsTag}"( bounds?.proj )
                if ( version == "1.3.0" )
                {
                  EX_GeographicBoundingBox {
                    westBoundLongitude( bounds?.minX )
                    eastBoundLongitude( bounds?.maxX )
                    southBoundLatitude( bounds?.minY )
                    northBoundLatitude( bounds?.maxY )
                  }
                }
                else
                {
                  LatLonBoundingBox(
                          minx: bounds?.minX,
                          miny: bounds?.minY,
                          maxx: bounds?.maxX,
                          maxy: bounds?.maxY
                  )
                }
              }
            }
          }
        }
      }
    }

    buffer = new StreamingMarkupBuilder( encoding: 'UTF-8' ).bind( x )?.toString()?.trim()

    Date endTime = new Date()
    responseTime = Math.abs(startTime.getTime() - endTime.getTime())

    requestInfoLog = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
            requestMethod: requestMethod, endTime: DateUtil.formatUTC(endTime), responseTime: responseTime,
            responseSize: buffer.getBytes().length, params: wmsParams.toString())

    log.info requestInfoLog.toString()

    [contentType: contentType, buffer: buffer]
  }

  static String toCamelCase(String text, boolean capitalized = false)
  {
    text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
    return capitalized ? capitalize( text ) : text
  }

  def testGeoScriptService(GetMapRequest wmsParams)
  {
    def wmsLayers = wmsParams?.layers?.split( ',' )

    wmsLayers?.each { wmsLayer ->
      def m = wmsLayer =~ /(\w+):(\w+)([\.:](\d+))?/
      if ( m )
      {
        def (prefix, name, id) = [m[0][1], m[0][2], m[0][4]]
      }
    }
  }

  def getMap(GetMapRequest wmsParams)
  {
    def requestType = "GET"
    def requestMethod = "GetMap"
    def OPTIMIZED_FORMAT = "image/vnd.jpeg-png"
    Date startTime = new Date()
    def responseTime
    def requestInfoLog
    def httpStatus
    def filename
    def bboxMidpoint
    def result
    Boolean addLocation = true

    Map<String, Object> omsParams = parseLayers( wmsParams )

    if ( omsParams )
    {
      Map<String, Object> bbox = parseBbox( wmsParams )

      omsParams += [
              cutWidth        : wmsParams.width,
              cutHeight       : wmsParams.height,
              outputFormat    : wmsParams.format,
              transparent     : wmsParams.transparent,
              operation       : "ortho",
              outputRadiometry: 'ossim_uint8'
      ]

      omsParams += parseStyles( wmsParams )

      if(!bbox.proj)
      {
        requestMethod = "getTile"
        omsParams.operation = "chip"
        double scaleX = wmsParams.width.toDouble()/(bbox.maxX-bbox.minX)
        double scaleY = wmsParams.height.toDouble()/(bbox.maxY-bbox.minY)
        bboxMidpoint = [y: (bbox.minY + bbox.maxY) / 2, x: (bbox.minX + bbox.maxX) / 2]
        omsParams.fullResXys = "${bboxMidpoint.x},${bboxMidpoint.y},${scaleX},${scaleY}"
        addLocation = false
      }
      else
      {
        // now add in the cut params for oms
        omsParams.cutWmsBbox = "${bbox.minX},${bbox.minY},${bbox.maxX},${bbox.maxY}"
        omsParams.srs = bbox?.proj.id

        bboxMidpoint = [lat: (bbox.minY + bbox.maxY) / 2, lon: (bbox.minX + bbox.maxX) / 2]
      }

      if ( omsParams.outputFormat == OPTIMIZED_FORMAT )
      {
        omsParams.outputFormat = optimalFormat(omsParams, bbox, wmsParams.version)
      }
      omsParams.remove( "rawCoords" )

      result = callOmsService( omsParams )
      httpStatus = result.status
      filename = omsParams.get( "images[0].file" )

      Date endTime = new Date()

      responseTime = Math.abs(startTime.getTime() - endTime.getTime())

      Map logParams = [
         timestamp: DateUtil.formatUTC(startTime),
         requestType: requestType,
         requestMethod: requestMethod,
         httpStatus: httpStatus,
         endTime: DateUtil.formatUTC(endTime),
         responseTime: responseTime,
         responseSize: result.buffer.length,
         filename: filename,
         bbox: bbox,
         params: wmsParams.toString(),
         location: bboxMidpoint
      ]

      if(addLocation)
      {
        logParams.location = bboxMidpoint
      }

      requestInfoLog = new JsonBuilder(logParams)
      log.info requestInfoLog.toString()
    }
    else
    {
        result = [
          buffer: createBlankImage(wmsParams),
          contentType: 'image/png',
          status: HttpStatus.OK
        ]
    }

    result
  }

  def createBlankImage(GetMapRequest wmsParams)
  {
    def image = new BufferedImage(wmsParams.width, wmsParams.height, BufferedImage.TYPE_INT_ARGB)
    def buffer = new FastByteArrayOutputStream()

    ImageIO.write(image, 'png', buffer)
    buffer.toByteArrayUnsafe()
  }

  def callOmsService(Map<String, Object> omsParams, def ogcParams = [:])
  {
    Map<String, Object> result = [status: HttpStatus.OK]

    // default histogram operation to auto-minmax
    //
    if ( !omsParams.histOp )
    {
      omsParams.histOp = autoHistogramMode ?: "auto-minmax"
    }

    if ( !omsParams.bands )
    {
      omsParams.bands = "default"
    }

    URL omsUrl = new URL( omsChipperUrl )

    omsParams += omsUrl.params
    omsUrl.setParams( omsParams )

    try
    {
      // call OMS and forward the response content and type
      HttpURLConnection connection = (HttpURLConnection) omsUrl.openConnection();
      Map responseMap = connection.headerFields

      String contentType
      if ( responseMap."Content-Type" )
      {
        contentType = responseMap."Content-Type"[0].split( ";" )[0]
      }


      FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream()
      result.status = connection.responseCode


      if ( connection.responseCode >= 400 )
      {
        String tempError = new String( outputStream?.toByteArrayUnsafe(), "UTF-8" )
        HashMap ogcExceptionResult = OgcExceptionUtil.formatOgcExceptionForResponse( ogcParams, "WMS server Error: ${tempError}" )

        //def ogcExcpetionResult = OgcExceptionUtil.formatWmsException(ogcParams)
        result.buffer = ogcExceptionResult.buffer
        result.contentType = ogcExceptionResult.contentType
      }
      else
      {
        outputStream << connection.inputStream
        connection.inputStream.close()
        outputStream.flush()
        outputStream.close()
        // We later need to map to an OGC exception.  For now we will just carry
        // the response on to this response
        //
        result.buffer = outputStream?.toByteArrayUnsafe()
        result.contentType = contentType

      }
    }
    catch (e)
    {

      e.printStackTrace()
      // log.error '*' * 40
      // log.error "${omsParams} ${ogcParams}"
      // log.error '*' * 40

      HashMap ogcExceptionResult = OgcExceptionUtil.formatOgcExceptionForResponse( ogcParams, "WMS server Error: ${e}" )

      // need to test OGC exception style
      result.status = HttpStatus.INTERNAL_SERVER_ERROR
      result.buffer = ogcExceptionResult.buffer
      result.contentType = ogcExceptionResult.contentType

      log.error e.message
    }

    result
  }

  Map<java.lang.String, java.lang.Object> parseBbox(GetMapRequest wmsParams)
  {
    def coords = wmsParams?.bbox?.split( ',' )?.collect { it.toDouble() }

    def proj = projections?.find {
      def id = (wmsParams.version == "1.3.0") ? wmsParams?.crs : wmsParams?.srs
      it.id?.equalsIgnoreCase( id )
    }

    def bbox

    if ( wmsParams.version == "1.3.0" && proj?.units == '\u00b0' )
    {
      bbox = [
              minX: coords[1],
              minY: coords[0],
              maxX: coords[3],
              maxY: coords[2],
              proj: proj
      ]
    }
    else
    {
      bbox = [
              minX: coords[0],
              minY: coords[1],
              maxX: coords[2],
              maxY: coords[3],
              proj: proj
      ]
    }

    bbox
  }

  private Map<String, Object> parseLayers(GetMapRequest wmsParams)
  {
    HashMap omsParams = [:]
    def layerNames = wmsParams?.layers?.split( ',' )
    Integer imageListIdx = 0

    layerNames?.each { layerName ->
      List images = fetchImages( layerName, wmsParams  )

      // add image chipper files for the oms params
      images.eachWithIndex { v, i ->
        omsParams."images[${imageListIdx}].file" = v.imageFile
        omsParams."images[${imageListIdx}].entry" = v.entry
        omsParams.rawCoords = v.imageCoords
        imageListIdx++
      }
    }

    if ( imageListIdx )
    {
      omsParams
    }
    else
    {
      [:]
    }
  }

  private Map<String, Object> parseStyles(GetMapRequest wmsParams)
  {
    def styles = [:]
    def newStyles = [:]

    if ( wmsParams?.styles?.trim() )
    {
      try
      {
        styles = new JsonSlurper().parseText( wmsParams?.styles )
      }
      catch (e)
      {
        e.printStackTrace()
      }
    }
    // chipper requires to be camel case
    // so change from snake to camelCase
    // if it's already camel it will not affect
    // the string
    styles?.each { k, v ->
      String newKey = toCamelCase( k )
      if ( newKey.toLowerCase().contains( "histcenter" ) )
      {
        if ( v.toBoolean() )
        {
          newStyles."histCenter" = v
        }
      }
      else
      {
        newStyles."${newKey}" = v
      }
    }

    newStyles
  }

  private List fetchImages(String layerName, GetMapRequest wmsParams)
  {
    List images = null
    def m = layerName =~ /(\w+):(\w+)([\.:](\d+))?/

    if ( m )
    {
      def (prefix, name, id) = [m[0][1], m[0][2], m[0][4]]
      def mosaicLimit = grailsApplication.config.omar.wms.mosaic.limit ?: "10"

// println "mosaicLimit: ${mosaicLimit}"
      def bbox = parseBbox(wmsParams)

      def queryParams = [
              filter: (id) ? "in(${id})" : wmsParams.filter,
              max: mosaicLimit?.toInteger()
      ]

      if ( bbox.proj )
      {
        queryParams.bbox = bbox
      }

      // added sensor_id, mission_id and file_type (for data type of source satellite image) and title for image ID
      images = geoscriptService.queryLayer( "${prefix}:${name}", queryParams )?.features?.inject( [] ) { a, b ->
        a << [
                id       : b.id,
                imageFile: b.filename ?: b.properties?.filename,
                entry    : b.entry_id ? b.entry_id?.toInteger() : b.properties?.entry_id?.toInteger(),
                access_date: b.access_date,
                imageCoords: b.geometry?.coordinates ?: b.ground_geom
        ]
        a
      }
      List<String> imageEntries = new ArrayList<String>(images.size)
      images.forEach { imageEntries.add(it.id.toString().replaceFirst("raster_entry.", "")) }
      // Remove updateLateAccessDates to test fix to WMS calls sometimes not returning
      //stagerService.updateLastAccessDates(imageEntries)
    }
    return images
  }

  private String optimalFormat(Map<String, Object> omsParams, Map<String, Object> bbox, String wmsVersion)
  {
    def tileGeom
    def imageGeom
    def sourceESPG = (omsParams?.srs && omsParams?.srs.equalsIgnoreCase("ESPG:3857")) ? 3857 : 4326
    def rawCoords = omsParams.get( "rawCoords" )[0][0]
    def geometryFactory = new GeometryFactory( new PrecisionModel( PrecisionModel.FLOATING ), sourceESPG )

    def tileCoords = [
      new Coordinate( bbox.minX, bbox.minY ),
      new Coordinate( bbox.minX, bbox.maxY ),
      new Coordinate( bbox.maxX, bbox.maxY ),
      new Coordinate( bbox.maxX, bbox.minY ),
      new Coordinate( bbox.minX, bbox.minY )
      ] as Coordinate[]

    def imageCoords = []
    
    if (wmsVersion == "1.3.0" && bbox?.proj?.units == '\u00b0') {
      rawCoords.each {
        imageCoords.push(new Coordinate( it[1], it[0] ))
      }
    }
    else {
      rawCoords.each {
        imageCoords.push(new Coordinate( it[0], it[1] ))
      }
    }
    
    tileGeom = geometryFactory.createPolygon( geometryFactory.createLinearRing( tileCoords ), null )
    imageGeom = geometryFactory.createPolygon( geometryFactory.createLinearRing( imageCoords as Coordinate[]), null )

    if (imageGeom.contains( tileGeom ))
    {
      return "image/jpeg"
    }
    else
    {
      return "image/png"
    }
  }

  def getStyles()
  {

  }

  def getLegendGraphic(def params)
  {
    def legendData = new JsonSlurper().parseText(footprintService.getFootprintsLegend(params))?.collect { style ->
        [label: style.label, color: Color.decode(style.color)]
    }

    def xOffset = 10
    def yOffset = 10
    def lineSize = 20
    def rectSize = 10

    def imageHeight = (legendData.size() * lineSize) + (2 * yOffset)
    def charSize = (legendData*.label*.length()).max() * yOffset
    def imageWidth = Math.min( charSize +  yOffset, 256 )

    // println "imageWidth: ${imageWidth}"
    // println "imageHeight: ${imageHeight}"

    def image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)

    def ostream = new FastByteArrayOutputStream(
      (image.sampleModel.sampleSize.sum() / 8 * image.width * image.height).intValue()
    )

    def g2d = image.createGraphics()

    g2d.color = Color.white
    g2d.fillRect(0, 0, image.width, image.height)

    // def url = "http://localhost:8081/footprints/getFootprintsLegend?style=bySensorType".toURL()
    // def legendData = new JsonSlurper().parseText(url.text)

    legendData.eachWithIndex { legendItem, index ->
        def y1 = (index * lineSize) + yOffset
        def y2 = (index * lineSize) + (2 * yOffset)

        g2d.color = legendItem.color
        g2d.fillRect(xOffset, y1, rectSize, rectSize)
        g2d.color = Color.black
        g2d.drawRect(xOffset, y1, rectSize, rectSize)
        g2d.setFont(new Font("Sans Serif", Font.BOLD, 12))
        g2d.drawString(legendItem.label, xOffset + rectSize + xOffset, y2)
    }

    g2d.dispose()

    ImageIO.write(image, 'png', ostream)

    return [contentType: 'image/png', file: ostream.toByteArrayUnsafe()]
  }
}

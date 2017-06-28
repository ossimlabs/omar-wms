package omar.wms

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.xml.StreamingMarkupBuilder
import omar.core.HttpStatus
import omar.core.OgcExceptionUtil
import org.springframework.beans.factory.InitializingBean

@Slf4j
class WebMappingService implements InitializingBean
{
  static transactional = false

  def grailsLinkGenerator
  def grailsApplication
  def geoscriptService

  def serverData
//  def layers
  def projections


  @Override
  void afterPropertiesSet() throws Exception
  {
    serverData        = grailsApplication.config?.geoscript?.serverData
//    layers = grailsApplication.config.geoscript.layers
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


    def x = {
      mkp.xmlDeclaration()

      if ( version == "1.1.1" )
      {
        mkp.yieldUnescaped """<!DOCTYPE WMT_MS_Capabilities SYSTEM "${docTypeLocation}">"""
      }

      def rootTag = ( version == "1.1.1" ) ? "WMT_MS_Capabilities" : "WMS_Capabilities"
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
              contentType = ( version == '1.1.1' ) ? "application/vnd.ogc.wms_xml" : "text/xml"
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
/*
            GetFeatureInfo {
              serverData.Capability.Request.GetFeatureInfo.Format.each { format ->
                Format( format )
              }
              DCPType {
                HTTP {
                  Get {
                    OnlineResource( 'xlink:type': "simple",
                        'xlink:href': grailsLinkGenerator.link( absolute: true, controller: 'wms', action: 'getFeatureInfo' ) )
                  }
                }
              }
            }
*/
          }
          Exception {
            serverData.Capability.Exception.Format.each { format ->
              Format( format )
            }
          }
          Layer {
            Title( serverData.Capability.Layer.Title )
            Abstract( serverData.Capability.Layer.Abstract )
            def crsTag = ( version == '1.1.1' ) ? "SRS" : "CRS"
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

    [contentType: contentType, buffer: buffer]
  }

  static String toCamelCase(String text, boolean capitalized = false)
  {
    text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
    return capitalized ? capitalize( text ) : text
  }

  def testGeoScriptService(GetMapRequest wmsParams)
  {
    println wmsParams

    def wmsLayers = wmsParams?.layers?.split( ',' )

    wmsLayers?.each { wmsLayer ->
      def m = wmsLayer =~ /(\w+):(\w+)([\.:](\d+))?/
      if ( m )
      {
        def (prefix, name, id) = [m[0][1], m[0][2], m[0][4]]

        println geoscriptService.queryLayer(
            "${prefix}:${name}",
            [
                filter: ( id ) ? "in(${id})" : wmsParams?.filter,
                fields: ['id', 'filename', 'entry_id']
            ]
        )
      }
    }
  }

  def getMap(GetMapRequest wmsParams)
  {
    String autoHistogramMode = grailsApplication?.config?.omar?.wms?.oms?.chipper?.histOp

    HashMap result = [status: HttpStatus.OK]
    String omsChipperUrl = grailsApplication?.config?.omar?.wms?.oms?.chipper?.url
    def otherParams = [startDate: new Date()]
    Integer imageListIdx = 0
    log.trace "getMap: Entered ................"
    otherParams.startTime = System.currentTimeMillis()
    otherParams.internalTime = otherParams.startTime
    HashMap omsParams = [:]

    def ostream = new ByteArrayOutputStream()
    def styles = [:]

    if ( wmsParams?.styles?.trim() )
    {
      try
      {
        styles = new JsonSlurper().parseText( wmsParams?.styles )
      }
      catch ( e )
      {
        e.printStackTrace()
      }
    }
    // chipper requires to be camel case
    // so change from snake to camelCase
    // if it's already camel it will not affect
    // the string
    styles.each { k, v ->
      String newKey = toCamelCase( k )
      if(newKey.toLowerCase().contains("histcenter"))
      {
        omsParams."histCenter" = v
      }
      else
      {
        omsParams."${newKey}" = v
      }
    }

    def layerNames = wmsParams?.layers?.split( ',' )

    layerNames?.each { layerName ->
      List images = fetchImages( layerName, wmsParams.filter )

////      println "images: ${images}"

      // add image chipper files for the oms params
      images.eachWithIndex { v, i ->
        omsParams."images[${i + imageListIdx}].file" = v.imageFile
        omsParams."images[${i + imageListIdx}].entry" = v.entry
        imageListIdx++
      }
    }

    def coords = wmsParams?.bbox?.split( ',' )?.collect { it.toDouble() }
    def proj = projections?.find {
      def id = ( wmsParams.version == "1.3.0" ) ? wmsParams?.crs : wmsParams?.srs
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

//    println bbox

    // now add in the cut params for oms
    omsParams.cutWmsBbox = "${bbox.minX},${bbox.minY},${bbox.maxX},${bbox.maxY}"
    omsParams.cutWidth = wmsParams.width
    omsParams.cutHeight = wmsParams.height
    omsParams.srs = bbox?.proj.id
    omsParams.outputFormat = wmsParams.format
    omsParams.transparent = wmsParams.transparent
    omsParams.operation = "ortho"
    omsParams.outputRadiometry = 'ossim_uint8'
    // default histgram operation to auto-minmax
    //
    if(!omsParams.histOp)
    {
      omsParams.histOp = autoHistogramMode?:"auto-minmax"
    }
    if(!omsParams.bands)
    {
      omsParams.bands="default"
    }
    URL omsUrl = new URL( "${omsChipperUrl}" )
    omsParams = omsParams + omsUrl.params
    //omsParams.each{k,v->omsParams."${k}" = v?.encodeAsURL()}
    omsUrl.setParams( omsParams )

    try
    {
      // call OMS and forward the response content and type
      HttpURLConnection connection = (HttpURLConnection)omsUrl.openConnection();
      Map responseMap = connection.headerFields;

      String contentType
      if(responseMap."Content-Type")
      {
        contentType = responseMap."Content-Type"[0].split( ";" )[0]
      }


      ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
      result.status = connection.responseCode


      if ( connection.responseCode >= 400 )
      {
        String tempError = new String( outputStream?.toByteArray(), "UTF-8" )
        HashMap ogcExceptionResult = OgcExceptionUtil.formatOgcExceptionForResponse( wmsParams, "WMS server Error: ${tempError}" )

        //def ogcExcpetionResult = OgcExceptionUtil.formatWmsException(wmsParams)
        result.buffer = ogcExceptionResult.buffer
        result.contentType = ogcExceptionResult.contentType
      }
      else
      {
        outputStream << connection.inputStream
        // We later need to map to an OGC exception.  For now we will just carry
        // the response on to this response
        //
        result.buffer = outputStream?.toByteArray()
        result.contentType = contentType

      }
    }
    catch ( e )
    {

      e.printStackTrace()

      HashMap ogcExceptionResult = OgcExceptionUtil.formatOgcExceptionForResponse( wmsParams, "WMS server Error: ${e}" )

      // need to test OGC exception style
      result.status = HttpStatus.INTERNAL_SERVER_ERROR
      result.buffer = ogcExceptionResult.buffer
      result.contentType = ogcExceptionResult.contentType

      log.error e.message
    }
    otherParams.internalTime = System.currentTimeMillis()
    //otherParams.endDate = new Date()
    result.metrics = otherParams
    log.trace "getMap: Leaving ................"

    result

  }

  private List fetchImages(String layerName, String filter=null)
  {
    List images = null
    def m = layerName =~ /(\w+):(\w+)([\.:](\d+))?/

    if ( m )
    {
      def (prefix, name, id) = [m[0][1], m[0][2], m[0][4]]

      images = geoscriptService.queryLayer(
          "${prefix}:${name}",
          [
              filter: ( id ) ? "in(${id})" : filter,
              fields: ['id', 'filename', 'entry_id']
          ]
      )?.features?.inject([]) { a, b ->
        a << [
          id: b.id,
          imageFile: b.filename  ?: b.properties?.filename,
          entry: b.entry_id ? b.entry_id?.toInteger() : b.properties?.entry_id?.toInteger()
        ]

        a
      }
    }

    images
  }
}

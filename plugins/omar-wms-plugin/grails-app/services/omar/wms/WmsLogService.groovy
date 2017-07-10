/**
 * @version 1.1.0
 */
package omar.wms

//import geoscript.geom.Bounds
//import geoscript.proj.Projection

import grails.transaction.Transactional

/**
 * @brief Logs GetMap request made to the service to the WmsLog.
 */
@Transactional
class WmsLogService
{
  def dataSourceUnproxied
  
  def logGetMapRequest(GetMapRequest wmsParams, def otherParams)
  {
    def wmsLog = new WmsLog(
        request: wmsParams.request,
        layers: wmsParams.layers,
        bbox: wmsParams.bbox,
        width: wmsParams.width,
        height: wmsParams.height,
        format: wmsParams.format,
        styles: wmsParams.styles,
        startDate: otherParams.startDate,
        endDate: otherParams.endDate,
        ip: otherParams.ip
    )
    if ( ( otherParams.internalTime != null ) &&
        ( otherParams.startTime != null ) &&
        ( otherParams.endTime != null ) )
    {
      wmsLog.internalTime = ( otherParams.internalTime - otherParams.startTime ) / 1000
      wmsLog.renderTime = ( otherParams.endTime - otherParams.internalTime ) / 1000
      wmsLog.totalTime = ( otherParams.endTime - otherParams.startTime ) / 1000
    }

//    def bounds = new Bounds( *( wmsParams?.bbox?.split( ',' )?.collect {
//      it.toDouble()
//    } ), wmsParams?.srs )
//
//    def epsg3857 = ( bounds?.proj?.id == 3857 ) ? bounds : bounds?.reproject( 'epsg:3857' )
//
//    wmsLog.geometry = bounds?.geometry?.g
//    wmsLog.meanGsd = ( epsg3857?.maxY - epsg3857?.minY ) / wmsLog?.height
//    wmsLog.geometry.setSRID( new Projection( wmsParams.srs )?.epsg )
    wmsLog.save()
  }

//  def insertStatement = """INSERT INTO wms_log (
//    bbox, end_date, format, geometry, height, internal_time, ip, layers, mean_gsd, render_time, request, start_date,
//    styles, total_time, url, user_name, width
//  ) VALUES (
//    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
//  )"""
}


  

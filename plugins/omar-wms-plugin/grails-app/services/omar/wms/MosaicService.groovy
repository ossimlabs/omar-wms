package omar.wms

import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import java.awt.Graphics
import java.awt.Rectangle
import java.awt.Font
import java.awt.FontMetrics

import org.springframework.util.FastByteArrayOutputStream

class MosaicService
{
  def geoscriptService
  def webMappingService

  def render(GetMapRequest wmsParams)
  {
//      println wmsParams

      def bbox = webMappingService.parseBbox( wmsParams )
      def blank = new BufferedImage(wmsParams.width, wmsParams.height, BufferedImage.TYPE_INT_ARGB)

      def wkt = "POLYGON ((${bbox.minX} ${bbox.minY}, ${bbox.minX} ${bbox.maxY}, ${bbox.maxX} ${bbox.maxY}, ${bbox.maxX} ${bbox.minY}, ${bbox.minX} ${bbox.minY}))"
      def filter = "acquisition_date is not null and intersection(ground_geom,${wkt}) = ${wkt}"

      def queryResults = geoscriptService.queryLayer(
          'omar:raster_entry', [
              bbox: bbox,
              filter: filter,
              max: 1,
              // fields: ['filename', 'entry_id'],
          	  sort: [['acquisition_date', 'DESC']]
          ]
      )

//      println queryResults

      def contentType = 'image/png'
      def imageType = 'png'
      def tileImage = null

      if ( queryResults?.numberOfFeatures == 1 )
      {
        contentType = 'image/jpeg'
        imageType = 'jpeg'

        def filename = queryResults?.features[0]?.filename ?: queryResults?.features[0]?.properties?.filename
        def entry_id =  queryResults.features[0]?.entry_id ?:  queryResults.features[0]?.properties?.entry_id

        Map<String,String>  omsParams = [
          cutWidth        : wmsParams.width,
          cutHeight       : wmsParams.height,
          outputFormat    : contentType,
          transparent     : false,
          operation       : "ortho",
          'images[0].file'   : filename,
          'images[0].entry'   :entry_id,
          cutWmsBbox: "${bbox.minX},${bbox.minY},${bbox.maxX},${bbox.maxY}",
          srs: bbox?.proj.id,
          outputRadiometry: 'ossim_uint8'
        ]


        def tileResults = webMappingService.callOmsService(omsParams)

        switch(tileResults.status)
        {
        case 400:
          println new String(tileResults.buffer)
          break
        default:
          def istream = new BufferedInputStream(new ByteArrayInputStream(tileResults?.buffer))
          def omsImage = ImageIO.read(istream)

          tileImage = new BufferedImage(wmsParams.width, wmsParams.height, BufferedImage.TYPE_INT_RGB)

          def conv = tileImage.createGraphics()

          conv.drawRenderedImage(omsImage, new AffineTransform())
          conv.dispose()
          break
        }
      }

      def outputImage = ( tileImage ?: blank)
      def g2d = outputImage?.createGraphics()

      g2d.color = Color.red
      g2d.drawRect(0, 0, outputImage.width, outputImage.height)

      if ( tileImage )
      {
        def tileMetadata = queryResults?.features[0]


        def title = tileMetadata?.title ?: tileMetadata?.properties?.title
        def label = title ?: tileMetadata?.toString()

        drawCenteredString(g2d, label,
          new Rectangle(outputImage.width, wmsParams.height),
          new Font("Sans Serif", Font.BOLD, 12) )
      }

      g2d.dispose()

      int bufferSize = ( contentType == 'image/jpeg') ? WebMappingService.DEFAULT_JPEG_SIZE : WebMappingService.DEFAULT_PNG_SIZE
      def ostream = new FastByteArrayOutputStream(bufferSize)

      ImageIO.write(outputImage, imageType, ostream)

      [contentType: contentType, file: ostream.toByteArrayUnsafe()]
  }

  void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
    // Get the FontMetrics
    FontMetrics metrics = g.getFontMetrics(font);
    // Determine the X coordinate for the text
    int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
    // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
//    int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
    int y = rect.y + ((rect.height - metrics.getHeight()) ) + metrics.getAscent();

    // Set the font
    g.setFont(font);
    // Draw the String
    g.drawString(text, x, y);
  }
}

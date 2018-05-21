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
      // println wmsParams

      def bbox = webMappingService.parseBbox( wmsParams )
      def blank = new BufferedImage(wmsParams.width, wmsParams.height, BufferedImage.TYPE_INT_ARGB)

      def wkt = "POLYGON ((${bbox.minX} ${bbox.minY}, ${bbox.minX} ${bbox.maxY}, ${bbox.maxX} ${bbox.maxY}, ${bbox.maxX} ${bbox.minY}, ${bbox.minX} ${bbox.minY}))"
      def filter = "acquisition_date is not null and intersection(ground_geom,${wkt}) = ${wkt}"

      def queryResults = geoscriptService.queryLayer(
          'omar:raster_entry', [
              bbox: bbox,
              filter: filter,
              max: 1,
          	   sort: [['acquisition_date', 'DESC']]
          ]
      )

      def contentType = 'image/gif'
      def imageType = 'gif'
      def tileImage = null

      if ( queryResults?.numberOfFeatures == 1 )
      {
        contentType = 'image/jpeg'
        imageType = 'jpeg'

        Map<String,String>  omsParams = [
          cutWidth        : wmsParams.width,
          cutHeight       : wmsParams.height,
          outputFormat    : contentType,
          transparent     : false,
          operation       : "ortho",
          'images[0].file'   : queryResults.features[0].filename,
          'images[0].entry'   : queryResults.features[0].entry_id,
          cutWmsBbox: "${bbox.minX},${bbox.minY},${bbox.maxX},${bbox.maxY}",
          srs: bbox?.proj.id,
          outputRadiometry: 'ossim_uint8'
        ]

        // println "*"*20
        // println omsParams
        // println "*"*20

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
      else
      {
        tileImage = blank
      }

      def outputImage = ( tileImage ?: blank)
      def g2d = outputImage?.createGraphics()

      g2d.color = Color.red
      g2d.drawRect(0, 0, outputImage.width, outputImage.height)

      // def label = "${queryResults.numberMatched}"

      if ( queryResults?.features )
      {
        def label = queryResults?.features[0]?.title ?:
          new File(queryResults?.features[0]?.filename)?.name

        drawCenteredString(g2d, label,
          new Rectangle(outputImage.width, wmsParams.height),
          new Font("Sans Serif", Font.BOLD, 12) )
      }

      g2d.dispose()

      def ostream = new FastByteArrayOutputStream(
        (outputImage.width * outputImage.height * 4).intValue()
      )

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

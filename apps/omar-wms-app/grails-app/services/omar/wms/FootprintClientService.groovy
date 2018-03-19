package omar.wms

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Value

@Transactional(readOnly=true)
class FootprintClientService
{
  @Value ('${omar.wms.geoscript.url}')
  def geoscriptEndpoint

  def getFootprintsLegend(def params)
  {
    def getFootprintsLegendEndpoint = geoscriptEndpoint.replace('geoscriptApi',
      'footprints/getFootprintsLegend')

    def url = "${getFootprintsLegendEndpoint}?style=${params.style}".toURL()

    url.text
  }
}

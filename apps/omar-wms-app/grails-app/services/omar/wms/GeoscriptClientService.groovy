package omar.wms

import grails.transaction.Transactional
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

import org.springframework.beans.factory.annotation.Value

@Transactional (readOnly = true)
class GeoscriptClientService
{
  @Value ('${omar.wms.geoscript.url}')
  def geoscriptEndpoint

  def getCapabilitiesData()
  {
    def url = "${geoscriptEndpoint}/getCapabilitiesData".toURL()

    new JsonSlurper().parse( url )

  }

  def listProjections()
  {
    def url = "${geoscriptEndpoint}/listProjections".toURL()

    new JsonSlurper().parse( url )
  }

  def queryLayer(String typeName, Map<String, Object> options, String resultType = 'results', String featureFormat = null)
  {
    def params = [
            typeName  : typeName,
            resultType: resultType
    ]

    if ( options.max )
    {
      params.max = options.max
    }

    if ( options.start )
    {
      params.start = options.start
    }

    if ( options.filter )
    {
      params.filter = options.filter
    }

    if ( featureFormat )
    {
      params.featureFormat = featureFormat
    }

    if ( options.fields )
    {
      params.fields = options.fields.join( ',' )
    }

    if ( options.sort )
    {
      params.sort = options.sort.collect { it.join( ' ' ) }.join( ',' )
    }

    if ( options.bbox )
    {
        params.bbox = JsonOutput.toJson(options.bbox)
    }

    def newParams = params.collect {
      "${it.key}=${URLEncoder.encode( it.value as String, 'UTF-8' )}"
    }.join( '&' )

    def url = "${geoscriptEndpoint}/queryLayer?${newParams}".toURL()

    new JsonSlurper().parse( url )
  }
}

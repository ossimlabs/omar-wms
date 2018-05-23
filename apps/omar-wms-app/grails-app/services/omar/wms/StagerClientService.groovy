package services.omar.wms

import grails.transaction.Transactional
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

@Transactional (readOnly = true)
class StagerClientService {
    @Value ('${omar.wms.geoscript.url}')
    String stagerEndpoint

    def updateLastAccessDates(List<String> rasterEntryIds) {
        String idCsv = rasterEntryIds.join(",")
        def url = "${stagerEndpoint}/updateAccessDates?rasterEntries=$idCsv"

        new JsonSlurper().parse(url)
    }
}

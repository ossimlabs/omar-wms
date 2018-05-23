package omar.wms

import grails.transaction.Transactional
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Value

@Transactional (readOnly = true)
class StagerClientService {
    @Value ('${omar.wms.stager.url}')
    String stagerEndpoint

    def updateLastAccessDates(List<String> rasterEntryIds) {
        String idCsv = rasterEntryIds.join(",")
        def url = "${stagerEndpoint}/updateAccessDates?rasterEntries=$idCsv"
        println "DEBUG: URL results = $url"
        new JsonSlurper().parse(url)
    }
}

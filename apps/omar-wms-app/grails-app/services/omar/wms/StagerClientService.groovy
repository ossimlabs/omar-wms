package omar.wms

import grails.transaction.Transactional
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Value

@Transactional(readOnly = true)
class StagerClientService {
    @Value('${omar.wms.stager.url}')
    String stagerEndpoint

    def updateLastAccessDates(List<String> rasterEntryIds) {
        if (!rasterEntryIds.isEmpty()) {
            String idCsv = rasterEntryIds.join(",")
            def url = "${stagerEndpoint}/updateAccessDates?rasterEntries=$idCsv".toURL()
            println "DEBUG: URL results = $url"
            new JsonSlurper().parse(url)
        }
    }
}

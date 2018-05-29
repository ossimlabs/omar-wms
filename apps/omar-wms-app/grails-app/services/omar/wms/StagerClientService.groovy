package omar.wms

import grails.transaction.Transactional
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Value

@Transactional(readOnly = true)
class StagerClientService {
    @Value('${omar.wms.stager.url}')
    String stagerEndpoint

    List<String> updateLastAccessDates(List<String> rasterEntryIds) {
        List<String> updatedRasters = []
        if (!rasterEntryIds.isEmpty()) {
            String idCsv = rasterEntryIds.join(",")
            URL url = "${stagerEndpoint}/updateAccessDates?rasterEntries=$idCsv".toURL()
            updatedRasters = url.getText().split(",")
        }
        return updatedRasters
    }
}

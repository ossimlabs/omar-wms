package services.omar.wms

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

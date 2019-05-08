package omar.wms

import grails.testing.services.ServiceUnitTest
import spock.lang.*
import omar.wms.WmsController

import java.nio.Buffer

class WmsControllerSpec extends Specification
{

    /*
        Here we want to test WebMappingService.getMap() method, mocking the calls to external services
     */
    void "test getMap(getPsm=true)"()
    {
        def wms = Spy(WebMappingService)

        GetMapRequest getMapRequest = new GetMapRequest() // required param for WebMappingService.getMap()
        getMapRequest.width = 256
        getMapRequest.height = 256
        getMapRequest.bbox = "1.1,1.1,1.1,1.1"
        getMapRequest.service = "WMS"
        getMapRequest.version = "1.1.1"
        getMapRequest.request = "getPsm"
        getMapRequest.srs = "EPSG:4326"
        getMapRequest.crs = null
        getMapRequest.bbox = '147.3211669921875,-42.879638671875,147.32391357421875,-42.87689208984375'
        getMapRequest.format = 'image/png'
        getMapRequest.layers = 'omar:raster_entry'
        getMapRequest.styles = "{\"bands\":\"default\",\"histCenterTile\":false,\"histOp\":\"auto-minmax\",\"resamplerFilter\":\"bilinear\",\"nullPixelFlip\":false}"
        getMapRequest.transparent = true
        getMapRequest.filter = 'INTERSECTS(ground_geom,POLYGON((146.9285294926882 -42.96513088145454, 146.9285294926882 -42.74156864994309, 147.4379233272598 -42.74156864994309, 147.4379233272598 -42.96513088145454, 146.9285294926882 -42.96513088145454)))'
        getMapRequest.exceptions = null
        getMapRequest.bgcolor = null
        getMapRequest.username = (null)

        Map<String, Object> parseLayersResult = [
                'images[0].file': '/data/s3/2009/02/05/00/ntf/05FEB09OV05010005V090205P0001912264B220000100282M_001508507.ntf',
                'images[0].entry': 0,
                'images[1].file': '/data/s3/2009/02/05/00/ntf/05FEB09OV05010005V090205M0001912264B220000100072M_001508507.ntf',
                'images[1].entry': 0,
                'rawCoords': "[[[[147.1655, -42.7832], [147.173, -42.7832], [147.1809, -42.7836], [147.1889, -42.784], [147.1963, -42.7839], [147.2034, -42.7835], [147.2104, -42.7831], [147.2175, -42.7827], [147.2249, -42.7825], [147.2322, -42.7823], [147.2396, -42.7822], [147.247, -42.7822], [147.2545, -42.7821], [147.262, -42.7821], [147.2695, -42.782], [147.277, -42.782], [147.2845, -42.782], [147.292, -42.782], [147.2998, -42.7822], [147.3079, -42.7828], [147.3152, -42.7826], [147.3225, -42.7823], [147.33, -42.7824], [147.3377, -42.7825], [147.3453, -42.7826], [147.3528, -42.7825], [147.3526, -42.7886], [147.3524, -42.7947], [147.3523, -42.8008], [147.3523, -42.8071], [147.3524, -42.8135], [147.3525, -42.8198], [147.3525, -42.8261], [147.3524, -42.8322], [147.3524, -42.8385], [147.3524, -42.8448], [147.3524, -42.851], [147.3525, -42.8573], [147.3525, -42.8636], [147.3526, -42.8699], [147.3525, -42.8761], [147.3526, -42.8824], [147.3526, -42.8887], [147.3526, -42.8949], [147.3527, -42.9012], [147.3527, -42.9075], [147.3529, -42.914], [147.353, -42.9202], [147.3528, -42.9264], [147.3529, -42.9326], [147.3529, -42.9389], [147.3455, -42.9391], [147.3383, -42.9393], [147.3311, -42.9396], [147.3234, -42.9395], [147.3156, -42.9393], [147.3083, -42.9395], [147.3009, -42.9395], [147.2933, -42.9395], [147.2859, -42.9396], [147.2784, -42.9397], [147.2708, -42.9396], [147.2633, -42.9397], [147.256, -42.9399], [147.2487, -42.9401], [147.2413, -42.9402], [147.2339, -42.9403], [147.2265, -42.9405], [147.2188, -42.9403], [147.2112, -42.9402], [147.204, -42.9405], [147.1968, -42.9408], [147.1892, -42.9408], [147.1817, -42.9408], [147.1741, -42.9408], [147.1663, -42.9405], [147.1663, -42.9343], [147.1666, -42.9282], [147.1668, -42.9222], [147.167, -42.916], [147.1664, -42.9093], [147.1663, -42.903], [147.1663, -42.8967], [147.1664, -42.8905], [147.1671, -42.8848], [147.1672, -42.8787], [147.167, -42.8722], [147.1666, -42.8657], [147.1663, -42.8591], [147.1662, -42.8527], [147.1661, -42.8464], [147.1661, -42.8401], [147.166, -42.8338], [147.166, -42.8275], [147.166, -42.8212], [147.1656, -42.8146], [147.165, -42.8078], [147.1649, -42.8015], [147.165, -42.7953], [147.1652, -42.7892], [147.1655, -42.7832]]]]"
        ]

        Map<String, Object> parseBboxResult = [
                'minX': 147.3321533203125,
                'minY': -42.88238525390625,
                'maxX': 147.33489990234375,
                'maxY': -42.879638671875,
                'proj': ['id': 'EPSG:4326', 'units': '\u00b0']
        ]

        Map<String, Object> callOmsServiceInput = [
                'images[0].file': '/data/s3/2009/02/05/00/ntf/05FEB09OV05010005V090205P0001912264B220000100282M_001508507.ntf',
                'images[0].entry': 0,
                'images[1].file': '/data/s3/2009/02/05/00/ntf/05FEB09OV05010005V090205M0001912264B220000100072M_001508507.ntf',
                'images[1].entry': 0,
                'cutWidth': 256,
                'cutHeight': 256,
                'outputFormat': 'image/png',
                'transparent': true,
                'operation': 'psm',
                'outputRadiometry': 'ossim_uint8',
                'bands': 'default',
                'histOp': 'auto-minmax',
                'resamplerFilter': 'bilinear',
                'nullPixelFlip': false,
                'cutWmsBbox': "147.3321533203125,-42.88238525390625,147.33489990234375,-42.879638671875",
                'srs': 'EPSG:4326'
        ]

        Map<String, Object> callOmsServiceResult = [
                'statuss': 200,
                'buffer': new byte[0],
                'contentType': 'image/png'
        ]

        when:
            wms.getMap(getMapRequest, true)

        then:
            1 * wms.parseLayers(getMapRequest) >> parseLayersResult
            1 * wms.parseBbox(getMapRequest) >> parseBboxResult
            1 * wms.callOmsService(callOmsServiceInput) >> callOmsServiceResult
    }
}

// Place your Spring DSL code here
import omar.wms.FootprintClientService
import omar.wms.GeoscriptClientService

beans = {
  geoscriptService(GeoscriptClientService)
  footprintService(FootprintClientService)
}

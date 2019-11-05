// Place your Spring DSL code here
import omar.wms.FootprintClientService
import omar.wms.GeoscriptClientService
import omar.wms.StagerClientService

beans = {
  geoscriptService(GeoscriptClientService)
  footprintService(FootprintClientService)
  stagerService(StagerClientService)
}

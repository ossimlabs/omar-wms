package omar.wms

import omar.core.BindUtil

class MosaicController
{
  def mosaicService

  def index()
  {
    GetMapRequest wmsParams =  new GetMapRequest()
    bindData(wmsParams, BindUtil.fixParamNames( GetMapRequest, params ))

    render mosaicService.render(wmsParams)
  }
}

import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  CanvasRenderer
])

export type { ECharts } from 'echarts/core'
export default echarts

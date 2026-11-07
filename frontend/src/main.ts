import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElMessage } from 'element-plus'
import { RequestError } from '@/shared/api/RequestError'

import App from './App.vue'
import router from './app/router'
import '@/shared/styles/global.scss'

const app = createApp(App)

// 请求错误已经由统一客户端提示；意外的界面异常仍记录原因并给出可见反馈。
app.config.errorHandler = (error) => {
  if (error instanceof RequestError) return
  console.error('界面操作发生异常', error)
  ElMessage.error('操作未完成，请重试或刷新页面')
}

app.use(createPinia())
app.use(router)
app.mount('#app')

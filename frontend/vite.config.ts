import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    /* Element Plus 按需自动导入 */
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern'
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }
          if (id.includes('echarts')) return 'vendor-echarts'
          if (id.includes('element-plus')) {
            const componentMatch = id.match(/[\\/]element-plus[\\/]es[\\/]components[\\/]([^\\/]+)/)
            if (componentMatch?.[1]) {
              return `vendor-ep-${componentMatch[1]}`
            }
            return 'vendor-element-plus-core'
          }
          if (id.includes('@element-plus/icons-vue')) return 'vendor-element-icons'
          if (id.includes('vue-router')) return 'vendor-router'
          if (id.includes('pinia')) return 'vendor-pinia'
          if (id.includes('axios') || id.includes('dayjs')) return 'vendor-utils'
          if (id.includes('/vue/') || id.includes('\\vue\\') || id.includes('@vue')) return 'vendor-vue'
          return 'vendor-misc'
        }
      }
    }
  },
  server: {
    port: 5173,
    /* 开发环境代理到后端 */
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/public': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})

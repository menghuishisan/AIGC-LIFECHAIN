import { onBeforeUnmount, ref } from 'vue'

/** 为页面读取提供持久错误状态，避免仅弹出瞬时提示后留下空白页。 */
export function usePageLoad() {
  const pageError = ref('')
  let disposed = false
  let version = 0
  onBeforeUnmount(() => {
    disposed = true
  })

  async function runPageLoad(load: (isCurrent: () => boolean) => Promise<unknown>) {
    const requestVersion = ++version
    const isCurrent = () => !disposed && requestVersion === version
    pageError.value = ''
    try {
      await load(isCurrent)
    } catch (error) {
      if (isCurrent())
        pageError.value = error instanceof Error ? error.message : '数据加载失败，请重试'
    }
  }

  return { pageError, runPageLoad }
}

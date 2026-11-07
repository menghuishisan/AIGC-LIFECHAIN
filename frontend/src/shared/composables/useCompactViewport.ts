import { onBeforeUnmount, ref } from 'vue'

/** 详情、表单共用窄屏断点，卸载页面时移除监听。 */
export function useCompactViewport() {
  const media = window.matchMedia('(max-width: 760px)')
  const compact = ref(media.matches)
  const update = () => {
    compact.value = media.matches
  }
  media.addEventListener('change', update)
  onBeforeUnmount(() => media.removeEventListener('change', update))
  return compact
}

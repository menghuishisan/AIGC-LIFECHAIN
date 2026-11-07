import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

/** 三个业务端共用抽屉键盘与焦点规则，断点与布局样式保持一致。 */
export function useNavigationDrawer() {
  const media = window.matchMedia('(max-width: 860px)')
  const mobileNavigation = ref(media.matches)
  const navOpen = ref(false)
  const navPanel = ref<HTMLElement>()
  const navToggle = ref<{ $el: HTMLButtonElement }>()
  let previousOverflow: string | null = null
  const updateViewport = () => {
    mobileNavigation.value = media.matches
    navOpen.value = false
  }

  function focusableElements() {
    return [
      ...(navPanel.value?.querySelectorAll<HTMLElement>('a, button, [tabindex="0"]') || [])
    ].filter((element) => element.getClientRects().length && !element.hasAttribute('disabled'))
  }

  function onKeydown(event: KeyboardEvent) {
    if (!mobileNavigation.value || !navOpen.value) return
    if (event.key === 'Escape') {
      event.preventDefault()
      navOpen.value = false
      return
    }
    if (event.key !== 'Tab') return
    const elements = focusableElements()
    const first = elements[0]
    const last = elements[elements.length - 1]
    if (!first || !last) {
      event.preventDefault()
      return
    }
    if (
      event.shiftKey &&
      (document.activeElement === first || !navPanel.value?.contains(document.activeElement))
    ) {
      event.preventDefault()
      last.focus()
    } else if (
      !event.shiftKey &&
      (document.activeElement === last || !navPanel.value?.contains(document.activeElement))
    ) {
      event.preventDefault()
      first.focus()
    }
  }

  watch(navOpen, async (open) => {
    if (open && mobileNavigation.value) {
      previousOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      await nextTick()
      focusableElements()[0]?.focus()
    } else {
      if (previousOverflow !== null) document.body.style.overflow = previousOverflow
      previousOverflow = null
      navToggle.value?.$el.focus()
    }
  })
  media.addEventListener('change', updateViewport)
  document.addEventListener('keydown', onKeydown)
  onBeforeUnmount(() => {
    if (previousOverflow !== null) document.body.style.overflow = previousOverflow
    media.removeEventListener('change', updateViewport)
    document.removeEventListener('keydown', onKeydown)
  })
  return { navOpen, navPanel, navToggle, mobileNavigation }
}

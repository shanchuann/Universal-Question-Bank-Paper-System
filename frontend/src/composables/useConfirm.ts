import { h, render } from 'vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

interface ConfirmOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'info' | 'warning' | 'danger'
}

let dialogInstance: any = null
let container: HTMLDivElement | null = null

function createDialog() {
  if (dialogInstance) return dialogInstance

  container = document.createElement('div')
  container.id = 'global-confirm-dialog'
  document.body.appendChild(container)

  const vnode = h(ConfirmDialog, { ref: 'dialog' })
  render(vnode, container)
  dialogInstance = vnode.component?.exposed

  return dialogInstance
}

export function useConfirm() {
  const confirm = async (options: ConfirmOptions | string): Promise<boolean> => {
    const dialog = createDialog()
    if (!dialog) return false

    const opts = typeof options === 'string' ? { message: options } : options
    return dialog.show(opts)
  }

  return { confirm }
}

// 也可以直接导出一个全局函数
export async function globalConfirm(options: ConfirmOptions | string): Promise<boolean> {
  const { confirm } = useConfirm()
  return confirm(options)
}

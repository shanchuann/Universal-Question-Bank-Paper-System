import { ref, watch } from 'vue'

const STORAGE_KEY = 'paper_basket'
const stored = localStorage.getItem(STORAGE_KEY)
const basket = ref<string[]>(stored ? JSON.parse(stored) : [])

watch(basket, (newVal) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(newVal))
}, { deep: true })

export const useBasket = () => {
  const addToBasket = (id: string) => {
    if (!basket.value.includes(id)) {
      basket.value.push(id)
    }
  }

  const removeFromBasket = (id: string) => {
    const index = basket.value.indexOf(id)
    if (index > -1) {
      basket.value.splice(index, 1)
    }
  }

  const isInBasket = (id: string) => basket.value.includes(id)

  const clearBasket = () => {
    basket.value = []
  }

  return {
    basket,
    addToBasket,
    removeFromBasket,
    isInBasket,
    clearBasket
  }
}

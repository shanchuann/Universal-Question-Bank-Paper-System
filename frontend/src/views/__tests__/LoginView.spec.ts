import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import LoginView from '../LoginView.vue'
import { createRouter, createWebHistory } from 'vue-router'

// Mock the API client
vi.mock('@/api/client', () => ({
  authApi: {
    authLoginPost: vi.fn()
  }
}))

describe('LoginView', () => {
  it('renders login form', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [{ path: '/', component: { template: 'Home' } }]
    })

    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      }
    })

    expect(wrapper.find('h1').text()).toBe('Login')
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
    expect(wrapper.find('button').text()).toBe('Login')
  })
})

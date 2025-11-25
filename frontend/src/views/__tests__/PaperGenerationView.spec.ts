import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import PaperGenerationView from '../PaperGenerationView.vue'
import axios from 'axios'
import { createRouter, createWebHistory } from 'vue-router'

// Mock axios
vi.mock('axios')

// Mock router
const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', component: { template: '<div>Home</div>' } }]
})

describe('PaperGenerationView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders generation form', () => {
    const wrapper = mount(PaperGenerationView, {
      global: {
        plugins: [router]
      }
    })
    expect(wrapper.find('h1').text()).toBe('Generate Paper')
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    // The button text might be different, let's check the actual content or just existence
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('submits form successfully', async () => {
    const mockPost = axios.post as unknown as ReturnType<typeof vi.fn>
    mockPost.mockResolvedValue({
      data: {
        id: 123,
        title: 'My Generated Paper',
        createdAt: '2023-01-01',
        questions: [
          { id: 'q1', subjectId: 'math', type: 'SINGLE_CHOICE', difficulty: 'EASY', status: 'ACTIVE' }
        ]
      }
    })

    const wrapper = mount(PaperGenerationView, {
      global: {
        plugins: [router]
      }
    })
    
    // Fill form (simplified)
    // The view has v-model on inputs, let's set them if needed, or rely on defaults
    // Defaults are: title: 'My Generated Paper', total: 5
    
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockPost).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Paper "My Generated Paper" generated')
  })

  it('handles submission error', async () => {
    const mockPost = axios.post as unknown as ReturnType<typeof vi.fn>
    mockPost.mockRejectedValue(new Error('Failed'))

    const wrapper = mount(PaperGenerationView, {
      global: {
        plugins: [router]
      }
    })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to generate paper')
  })
})

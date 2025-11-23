import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import PaperGenerationView from '../PaperGenerationView.vue'

// Mock the API client
vi.mock('@/api/client', () => ({
  paperApi: {
    papersAutoPost: vi.fn()
  }
}))

import { paperApi } from '@/api/client'

describe('PaperGenerationView', () => {
  it('renders generation form', () => {
    const wrapper = mount(PaperGenerationView)
    expect(wrapper.find('h1').text()).toBe('Generate Paper')
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.find('button').text()).toBe('Generate')
  })

  it('submits form successfully', async () => {
    const mockPost = paperApi.papersAutoPost as unknown as ReturnType<typeof vi.fn>
    mockPost.mockResolvedValue({})

    const wrapper = mount(PaperGenerationView)
    
    // Fill form (simplified)
    await wrapper.find('input[type="text"]').setValue('math') // Subject ID
    
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockPost).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Paper generated successfully!')
  })

  it('handles submission error', async () => {
    const mockPost = paperApi.papersAutoPost as unknown as ReturnType<typeof vi.fn>
    mockPost.mockRejectedValue(new Error('Failed'))

    const wrapper = mount(PaperGenerationView)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to generate paper.')
  })
})

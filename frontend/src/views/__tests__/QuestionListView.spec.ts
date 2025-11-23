import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import QuestionListView from '../QuestionListView.vue'

// Mock the API client
vi.mock('@/api/client', () => ({
  questionApi: {
    questionsGet: vi.fn()
  }
}))

import { questionApi } from '@/api/client'

describe('QuestionListView', () => {
  it('renders question list', async () => {
    const mockQuestions = [
      { id: '1', type: 'SINGLE_CHOICE', difficulty: 'EASY', status: 'APPROVED', subjectId: 'math' },
      { id: '2', type: 'MULTI_CHOICE', difficulty: 'HARD', status: 'DRAFT', subjectId: 'english' }
    ]
    
    // Setup mock response
    const mockGet = questionApi.questionsGet as unknown as ReturnType<typeof vi.fn>
    mockGet.mockResolvedValue({
      data: {
        items: mockQuestions,
        totalElements: 2,
        totalPages: 1
      }
    })

    const wrapper = mount(QuestionListView)
    
    // Initial loading state
    // Note: flushPromises might be needed if the component does async setup immediately
    // but here we want to catch the loading state before it resolves
    
    // Wait for async operations
    await flushPromises()

    // Check if loading is gone
    expect(wrapper.text()).not.toContain('Loading...')

    // Check table headers
    expect(wrapper.find('th').text()).toBe('ID')
    
    // Check data rows
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
    expect(rows[0].text()).toContain('SINGLE_CHOICE')
    expect(rows[1].text()).toContain('MULTI_CHOICE')
  })

  it('handles error state', async () => {
    // Setup mock error
    const mockGet = questionApi.questionsGet as unknown as ReturnType<typeof vi.fn>
    mockGet.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(QuestionListView)
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load questions.')
  })
})

import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import QuestionListView from '../QuestionListView.vue'
import { createRouter, createWebHistory } from 'vue-router'

// Mock the API client
vi.mock('@/api/client', () => ({
  questionApi: {
    apiQuestionsGet: vi.fn()
  },
  knowledgePointApi: {
    apiKnowledgePointsGet: vi.fn().mockResolvedValue({ data: [] })
  }
}))

import { questionApi } from '@/api/client'

describe('QuestionListView', () => {
  it('renders question list', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [{ path: '/', component: { template: 'Home' } }]
    })

    const mockQuestions = [
      { id: '1', type: 'SINGLE_CHOICE', difficulty: 'EASY', status: 'APPROVED', subjectId: 'math', stem: 'Question 1' },
      { id: '2', type: 'MULTI_CHOICE', difficulty: 'HARD', status: 'DRAFT', subjectId: 'english', stem: 'Question 2' }
    ]
    
    // Setup mock response
    const mockGet = questionApi.apiQuestionsGet as unknown as ReturnType<typeof vi.fn>
    mockGet.mockResolvedValue({
      data: {
        content: mockQuestions,
        totalElements: 2,
        totalPages: 1
      }
    })

    const wrapper = mount(QuestionListView, {
      global: {
        plugins: [router],
        stubs: ['GoogleSelect']
      }
    })
    
    // Initial loading state
    // Note: flushPromises might be needed if the component does async setup immediately
    // but here we want to catch the loading state before it resolves
    
    // Wait for async operations
    await flushPromises()

    // Check if loading is gone
    expect(wrapper.text()).not.toContain('Loading...')

    // Check table headers - The first header is checkbox, second is Subject
    const headers = wrapper.findAll('th')
    expect(headers[1].text()).toBe('Subject')
    
    // Check data rows
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
    expect(rows[0]!.text()).toContain('SINGLE_CHOICE')
    expect(rows[1]!.text()).toContain('MULTI_CHOICE')
  })

  it('handles error state', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [{ path: '/', component: { template: 'Home' } }]
    })

    // Setup mock error
    const mockGet = questionApi.apiQuestionsGet as unknown as ReturnType<typeof vi.fn>
    mockGet.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(QuestionListView, {
      global: {
        plugins: [router],
        stubs: ['GoogleSelect']
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load questions.') // Note: The actual error message in component might be just the error string or "Failed to load questions" depending on logic.
    // Looking at component: <p>{{ error }}</p>. And error.value is set to 'Failed to load questions' in catch block usually?
    // Wait, let's check QuestionListView.vue error handling.
    // const fetchQuestions = async () => { ... } catch (err) { error.value = 'Failed to load questions.' }
    // So it should be 'Failed to load questions.'
  })
})

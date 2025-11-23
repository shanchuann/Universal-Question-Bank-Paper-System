import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ExamView from '../ExamView.vue'

// Mock the API client
vi.mock('@/api/client', () => ({
  examApi: {
    examsAccessCodeStartPost: vi.fn(),
    examsSessionIdSubmitPost: vi.fn()
  }
}))

import { examApi } from '@/api/client'

describe('ExamView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders start screen initially', () => {
    const wrapper = mount(ExamView)
    expect(wrapper.find('h1').text()).toBe('Start Exam')
    expect(wrapper.find('input[placeholder="Enter access code"]').exists()).toBe(true)
  })

  it('starts exam successfully', async () => {
    const mockStart = examApi.examsAccessCodeStartPost as unknown as ReturnType<typeof vi.fn>
    mockStart.mockResolvedValue({
      data: {
        sessionId: 'sess-123',
        timeRemainingSeconds: 3600,
        questions: [
          {
            questionId: 'q1',
            type: 'SINGLE_CHOICE',
            stem: 'What is 1+1?',
            options: [
              { key: 'A', text: '1' },
              { key: 'B', text: '2' }
            ]
          }
        ]
      }
    })

    const wrapper = mount(ExamView)
    
    // Enter access code
    await wrapper.find('input').setValue('CODE123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // Check if exam screen is shown
    expect(wrapper.find('.exam-screen').exists()).toBe(true)
    expect(wrapper.find('.stem').text()).toBe('What is 1+1?')
    expect(wrapper.findAll('.option')).toHaveLength(2)
  })

  it('handles start exam error', async () => {
    const mockStart = examApi.examsAccessCodeStartPost as unknown as ReturnType<typeof vi.fn>
    mockStart.mockRejectedValue(new Error('Invalid code'))

    const wrapper = mount(ExamView)
    await wrapper.find('input').setValue('INVALID')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to start exam')
  })
})

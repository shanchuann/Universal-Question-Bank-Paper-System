import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ExamView from '../ExamView.vue'
import axios from 'axios'
import { createRouter, createWebHistory } from 'vue-router'

// Mock axios
vi.mock('axios')

// Mock requestFullscreen
Element.prototype.requestFullscreen = vi.fn().mockResolvedValue(undefined)
// Mock exitFullscreen
Document.prototype.exitFullscreen = vi.fn().mockResolvedValue(undefined)

// Mock router
const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', component: { template: '<div>Home</div>' } }]
})

describe('ExamView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders start screen initially', () => {
    const wrapper = mount(ExamView, {
      global: {
        plugins: [router]
      }
    })
    expect(wrapper.find('h1').text()).toBe('Start Exam')
    expect(wrapper.find('input[placeholder="Enter Paper ID"]').exists()).toBe(true)
  })

  it('starts exam successfully', async () => {
    const mockPost = axios.post as unknown as ReturnType<typeof vi.fn>
    mockPost.mockResolvedValue({
      data: {
        id: 1,
        paper: {
          id: 101,
          title: 'Test Paper',
          questions: [
            {
              id: 'q1',
              stem: 'What is 1+1?',
              type: 'SINGLE_CHOICE',
              options: ['1', '2', '3', '4']
            }
          ]
        },
        userId: 'user-1',
        score: null
      }
    })

    const wrapper = mount(ExamView, {
      global: {
        plugins: [router]
      }
    })
    
    // Enter Paper ID
    await wrapper.find('input[placeholder="Enter Paper ID"]').setValue('101')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // Check if exam screen is shown
    expect(wrapper.find('.exam-screen').exists()).toBe(true)
    expect(wrapper.find('h2').text()).toContain('Test Paper')
    expect(wrapper.find('.question-content').text()).toBe('What is 1+1?')
  })

  it('submits exam and shows score', async () => {
    const mockPost = axios.post as unknown as ReturnType<typeof vi.fn>
    // Mock start exam response
    mockPost.mockResolvedValueOnce({
      data: {
        id: 1,
        paper: {
          id: 101,
          title: 'Test Paper',
          questions: [
            {
              id: 'q1',
              stem: 'What is 1+1?',
              type: 'SINGLE_CHOICE',
              options: ['1', '2']
            }
          ]
        },
        userId: 'user-1',
        score: 100,
        records: [
          { questionId: 'q1', userAnswer: '2', isCorrect: true }
        ]
      }
    })

    // Mock submit exam response
    mockPost.mockResolvedValueOnce({
      data: {
        id: 1,
        paper: {
          id: 101,
          title: 'Test Paper',
          questions: [
            {
              id: 'q1',
              stem: 'What is 1+1?',
              type: 'SINGLE_CHOICE',
              options: ['1', '2']
            }
          ]
        },
        userId: 'user-1',
        score: 100,
        records: [
          { questionId: 'q1', userAnswer: '2', isCorrect: true }
        ]
      }
    })

    const wrapper = mount(ExamView, {
      global: {
        plugins: [router]
      }
    })

    // Start exam
    await wrapper.find('input[placeholder="Enter Paper ID"]').setValue('101')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // Select answer
    const radio = wrapper.find('input[type="radio"]')
    if (radio.exists()) {
      await radio.setValue('2')
    }
    
    // Submit exam
    const buttons = wrapper.findAll('button')
    const submitBtn = buttons.find(b => b.text().includes('Submit Exam'))
    expect(submitBtn?.exists()).toBe(true)
    await submitBtn?.trigger('click')
    await flushPromises()

    // Check score display
    expect(wrapper.find('.score-value').text()).toBe('100')
    expect(wrapper.find('.status-badge.correct').exists()).toBe(true)
    
    const exitBtn = wrapper.findAll('button').find(b => b.text().includes('Exit Exam'))
    expect(exitBtn?.exists()).toBe(true)
  })
})

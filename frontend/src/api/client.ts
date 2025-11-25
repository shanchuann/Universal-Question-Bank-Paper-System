import { Configuration, AuthApi, QuestionBankApi, PaperApi, ExamApi, KnowledgePointApi, AnalyticsApi } from './generated'

const config = new Configuration({
  basePath: '/api',
})

export const authApi = new AuthApi(config)
export const questionApi = new QuestionBankApi(config)
export const paperApi = new PaperApi(config)
export const examApi = new ExamApi(config)
export const knowledgePointApi = new KnowledgePointApi(config)
export const analyticsApi = new AnalyticsApi(config)


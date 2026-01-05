import { Configuration, AuthApi, QuestionBankApi, PaperApi, ExamApi, KnowledgePointApi, AnalyticsApi } from './generated'

// 使用当前域名（开发时为 localhost:5173，由 Vite 代理转发到后端）
const config = new Configuration({
  basePath: window.location.origin,  // 这将使用当前页面的域名
})

export const authApi = new AuthApi(config)
export const questionApi = new QuestionBankApi(config)
export const paperApi = new PaperApi(config)
export const examApi = new ExamApi(config)
export const knowledgePointApi = new KnowledgePointApi(config)
export const analyticsApi = new AnalyticsApi(config)


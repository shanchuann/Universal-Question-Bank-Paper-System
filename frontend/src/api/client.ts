import { Configuration, AuthApi, QuestionBankApi, PaperApi } from './generated'

const config = new Configuration({
  basePath: '/api/v1',
})

export const authApi = new AuthApi(config)
export const questionApi = new QuestionBankApi(config)
export const paperApi = new PaperApi(config)

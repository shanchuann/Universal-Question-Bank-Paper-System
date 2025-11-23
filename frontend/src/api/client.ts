import { Configuration, AuthApi, QuestionBankApi } from './generated'

const config = new Configuration({
  basePath: '/api/v1',
})

export const authApi = new AuthApi(config)
export const questionApi = new QuestionBankApi(config)

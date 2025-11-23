import { Configuration, AuthApi } from './generated'

const config = new Configuration({
  basePath: '/api/v1',
})

export const authApi = new AuthApi(config)

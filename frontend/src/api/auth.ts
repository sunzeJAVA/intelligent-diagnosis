import { controlApi } from './client'

export interface UserInfo {
  username: string
  authorities: string[]
}

/**
 * 获取当前登录用户信息
 */
export async function fetchCurrentUser(): Promise<UserInfo> {
  const response = await controlApi.get<UserInfo>('/auth/me')
  return response.data
}

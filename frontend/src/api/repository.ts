import { dataApi } from './client'

export interface RepositoryConfig {
  id?: string
  name: string
  displayName: string
  type: 'GIT' | 'GITHUB' | 'GITLAB' | 'GITEE' | 'BITBUCKET' | 'LOCAL'
  url: string
  branch: string
  localPath: string
  enabled: boolean
  authType: 'NONE' | 'TOKEN' | 'SSH_KEY' | 'USERNAME_PASSWORD'
  authToken?: string
  authUsername?: string
  authPassword?: string
  authSshKeyPath?: string
  createdAt?: string
  updatedAt?: string
}

export interface SyncState {
  id: string
  repositoryId: string
  status: 'PENDING' | 'SYNCING' | 'SUCCESS' | 'FAILED'
  startedAt?: string
  completedAt?: string
  latestCommit?: string
  previousCommit?: string
  changedFiles?: number
  errorMessage?: string
  triggerType: 'INITIAL' | 'SCHEDULED' | 'MANUAL' | 'WEBHOOK'
  triggeredBy?: string
  createdAt: string
}

export async function listRepositories(): Promise<RepositoryConfig[]> {
  const response = await dataApi.get<RepositoryConfig[]>('/repositories')
  return response.data
}

export async function createRepository(config: RepositoryConfig): Promise<RepositoryConfig> {
  const response = await dataApi.post<RepositoryConfig>('/repositories', config)
  return response.data
}

export async function syncRepository(id: string): Promise<SyncState> {
  const response = await dataApi.post<SyncState>(`/repositories/${id}/sync`)
  return response.data
}

export async function getSyncHistory(id: string): Promise<SyncState[]> {
  const response = await dataApi.get<SyncState[]>(`/repositories/${id}/sync-history`)
  return response.data
}

export async function deleteRepository(id: string): Promise<void> {
  await dataApi.delete(`/repositories/${id}`)
}

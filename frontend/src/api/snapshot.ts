import { dataApi } from './client'

export interface SnapshotDto {
  id: string
  repositoryName: string
  commitHash: string
  status: string
  elementCount: number
  relationCount: number
  beforeSnapshotId: string | null
  afterSnapshotId: string | null
  createdAt: string
  completedAt: string | null
  workflowId: string | null
  qdrantSnapshotPath: string | null
  neo4jBackupPath: string | null
}

export interface SnapshotDiff {
  leftSnapshotId: string
  rightSnapshotId: string
  repositoryName: string
  leftCommitHash: string
  rightCommitHash: string
  leftElementCount: number
  rightElementCount: number
  elementDelta: number
  leftRelationCount: number
  rightRelationCount: number
  relationDelta: number
  leftStatus: string
  rightStatus: string
}

export async function listSnapshots(repository: string): Promise<SnapshotDto[]> {
  const response = await dataApi.get<SnapshotDto[]>('/snapshots', { params: { repository } })
  return response.data
}

export async function getSnapshot(id: string): Promise<SnapshotDto> {
  const response = await dataApi.get<SnapshotDto>(`/snapshots/${id}`)
  return response.data
}

export async function diffSnapshots(leftId: string, rightId: string): Promise<SnapshotDiff> {
  const response = await dataApi.get<SnapshotDiff>(`/snapshots/${leftId}/diff/${rightId}`)
  return response.data
}

export async function rollbackSnapshot(id: string): Promise<void> {
  await dataApi.post(`/snapshots/${id}/rollback`)
}

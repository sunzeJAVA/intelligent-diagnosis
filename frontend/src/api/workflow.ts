import { controlApi } from './client'

export interface WorkflowDto {
  workflowId: string
  workflowType: string
  status: string
  currentStep: string | null
  startedAt: string | null
}

export interface StartWorkflowRequest {
  repositoryId: string
  repositoryName: string
  branch: string
  commitHash: string
  commitMessage: string
  author: string
  previousCommit: string
  changedFiles: string[]
  repoPath: string
  language: string
  triggeredBy: string
}

export interface WorkflowStartResponse {
  workflowId: string
}

export async function listWorkflows(): Promise<WorkflowDto[]> {
  const response = await controlApi.get<WorkflowDto[]>('/workflows')
  return response.data
}

export async function getWorkflow(workflowId: string): Promise<WorkflowDto> {
  const response = await controlApi.get<WorkflowDto>(`/workflows/${workflowId}`)
  return response.data
}

export async function pauseWorkflow(workflowId: string): Promise<void> {
  await controlApi.post(`/workflows/${workflowId}/pause`)
}

export async function resumeWorkflow(workflowId: string): Promise<void> {
  await controlApi.post(`/workflows/${workflowId}/resume`)
}

export async function rollbackWorkflow(workflowId: string): Promise<void> {
  await controlApi.post(`/workflows/${workflowId}/rollback`)
}

export async function startWorkflow(request: StartWorkflowRequest): Promise<WorkflowStartResponse> {
  const response = await controlApi.post<WorkflowStartResponse>('/workflows/start', request)
  return response.data
}

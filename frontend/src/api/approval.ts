import { controlApi } from './client'

export interface ApprovalDto {
  workflowId: string
  repository: string
  commitHash: string
  riskLevel: string
  status: string
}

export interface ApprovalRequest {
  approver: string
  comment: string
}

export interface RejectionRequest {
  reason: string
}

export async function listPendingApprovals(): Promise<ApprovalDto[]> {
  const response = await controlApi.get<ApprovalDto[]>('/approvals')
  return response.data
}

export async function approveWorkflow(workflowId: string, request: ApprovalRequest): Promise<void> {
  await controlApi.post(`/approvals/${workflowId}/approve`, request)
}

export async function rejectWorkflow(workflowId: string, request: RejectionRequest): Promise<void> {
  await controlApi.post(`/approvals/${workflowId}/reject`, request)
}

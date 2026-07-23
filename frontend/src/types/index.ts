export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
}

export interface DiagnosisRequest {
  query: string
  errorInfo?: string
  service: string
}

export interface DiagnosisResponse {
  summary: string
  rootCause: string
  suggestions: string[]
  relatedCode: CodeSnippet[]
}

export interface CodeSnippet {
  filePath: string
  startLine: number
  endLine: number
  content: string
}

export interface WorkflowState {
  workflowId: string
  workflowType: string
  status: string
  currentStep: string
  startedAt: string
}

export interface ApprovalRequest {
  workflowId: string
  repository: string
  commitHash: string
  riskLevel: string
  status: string
}

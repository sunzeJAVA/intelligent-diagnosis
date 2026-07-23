import { dataApi } from './client'
import type { ApiResponse, DiagnosisRequest, DiagnosisResponse } from '@/types'

export async function diagnose(request: DiagnosisRequest): Promise<ApiResponse<DiagnosisResponse>> {
  const response = await dataApi.post<ApiResponse<DiagnosisResponse>>('/diagnosis', request)
  return response.data
}

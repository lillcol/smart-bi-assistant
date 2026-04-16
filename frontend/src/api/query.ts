import { http } from './http'
import type { QueryRequest, QueryResponse } from '../types/query'

export const queryByNl = async (payload: QueryRequest): Promise<QueryResponse> => {
  const { data } = await http.post<QueryResponse>('/query', payload)
  return data
}

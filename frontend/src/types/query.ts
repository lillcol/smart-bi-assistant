export interface QueryRequest {
  question: string
}

export interface QueryResponse {
  sql: string
  data: Record<string, unknown>[]
  explanation: string
}

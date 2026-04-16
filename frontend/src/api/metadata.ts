import { http } from './http'
import type { MetricDefinition, TableColumn } from '../types/metadata'

export const getDatabases = async (): Promise<string[]> => {
  const { data } = await http.get<string[]>('/metadata/databases')
  return data
}

export const getTables = async (database: string): Promise<string[]> => {
  const { data } = await http.get<string[]>('/metadata/tables', { params: { database } })
  return data
}

export const getTableSchema = async (database: string, table: string): Promise<TableColumn[]> => {
  const { data } = await http.get<TableColumn[]>('/metadata/table-schema', {
    params: { database, table }
  })
  return data
}

export const getMetrics = async (): Promise<MetricDefinition[]> => {
  const { data } = await http.get<MetricDefinition[]>('/metadata/metrics')
  return data
}

export const getPreview = async (
  database: string,
  table: string,
  limit = 10
): Promise<Record<string, unknown>[]> => {
  const { data } = await http.get<Record<string, unknown>[]>('/metadata/preview', {
    params: { database, table, limit }
  })
  return data
}

export interface TableColumn {
  columnName: string
  dataType: string
  columnDesc: string
}

export interface MetricDefinition {
  id: number
  metricCode: string
  metricName: string
  metricSql?: string
  description?: string
}

import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { getDatabases, getMetrics, getPreview, getTableSchema, getTables } from '../api/metadata'
import { queryByNl } from '../api/query'
import type { MetricDefinition, TableColumn } from '../types/metadata'

export const useQueryPage = () => {
  const databases = ref<string[]>([])
  const tables = ref<string[]>([])
  const selectedDatabase = ref('')
  const selectedTable = ref('')

  const previewRows = ref<Record<string, unknown>[]>([])
  const schemaRows = ref<TableColumn[]>([])
  const metricRows = ref<MetricDefinition[]>([])

  const question = ref('')
  const resultRows = ref<Record<string, unknown>[]>([])
  const resultSql = ref('')
  const resultExplanation = ref('')

  const loadingMeta = ref(false)
  const loadingPreview = ref(false)
  const loadingQuery = ref(false)

  const previewColumns = computed(() =>
    previewRows.value.length ? Object.keys(previewRows.value[0]) : []
  )
  const resultColumns = computed(() =>
    resultRows.value.length ? Object.keys(resultRows.value[0]) : []
  )

  const loadDatabases = async () => {
    loadingMeta.value = true
    try {
      databases.value = await getDatabases()
      if (databases.value.length > 0) {
        selectedDatabase.value = databases.value.includes('smart_bi') ? 'smart_bi' : databases.value[0]
      }
      if (selectedDatabase.value) {
        await loadTables()
      }
    } catch {
      ElMessage.error('加载数据库列表失败，请检查后端服务和代理配置')
    } finally {
      loadingMeta.value = false
    }
  }

  const loadTables = async () => {
    if (!selectedDatabase.value) return
    try {
      tables.value = await getTables(selectedDatabase.value)
      selectedTable.value = tables.value.includes('orders') ? 'orders' : tables.value[0] ?? ''
    } catch {
      ElMessage.error('加载数据表列表失败')
    }
  }

  const loadBrowseData = async () => {
    if (!selectedDatabase.value || !selectedTable.value) {
      ElMessage.warning('请先选择数据库和数据表')
      return
    }
    loadingPreview.value = true
    try {
      const [preview, schema, metrics] = await Promise.all([
        getPreview(selectedDatabase.value, selectedTable.value, 10),
        getTableSchema(selectedDatabase.value, selectedTable.value),
        getMetrics()
      ])
      previewRows.value = preview
      schemaRows.value = schema
      metricRows.value = metrics
    } catch (e) {
      ElMessage.error('加载数据失败')
    } finally {
      loadingPreview.value = false
    }
  }

  const runNaturalQuery = async () => {
    if (!question.value.trim()) {
      ElMessage.warning('请输入自然语言问题')
      return
    }
    loadingQuery.value = true
    try {
      const resp = await queryByNl({ question: question.value.trim() })
      resultRows.value = resp.data ?? []
      resultSql.value = resp.sql ?? ''
      resultExplanation.value = resp.explanation ?? ''
    } catch (e) {
      let message = '自然语言查询失败'
      if (axios.isAxiosError(e)) {
        const backendMessage = (e.response?.data as { message?: string } | undefined)?.message
        if (backendMessage) {
          message = backendMessage
        } else if (e.message) {
          message = e.message
        }
      }
      ElMessage.error(message)
    } finally {
      loadingQuery.value = false
    }
  }

  const clearQuery = () => {
    question.value = ''
    resultRows.value = []
    resultSql.value = ''
    resultExplanation.value = ''
  }

  return {
    databases,
    tables,
    selectedDatabase,
    selectedTable,
    previewRows,
    previewColumns,
    schemaRows,
    metricRows,
    question,
    resultRows,
    resultColumns,
    resultSql,
    resultExplanation,
    loadingMeta,
    loadingPreview,
    loadingQuery,
    loadDatabases,
    loadTables,
    loadBrowseData,
    runNaturalQuery,
    clearQuery
  }
}

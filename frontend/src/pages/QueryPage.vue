<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useQueryPage } from '../composables/useQueryPage'

const page = reactive(useQueryPage())

const copySql = async () => {
  if (!page.resultSql) {
    ElMessage.warning('暂无 SQL 可复制')
    return
  }
  await navigator.clipboard.writeText(page.resultSql)
  ElMessage.success('SQL 已复制')
}

onMounted(async () => {
  await page.loadDatabases()
  await page.loadBrowseData()
})
</script>

<template>
  <div class="page">
    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-title">数据浏览</div>
      </template>
      <div class="toolbar">
        <el-select
          v-model="page.selectedDatabase"
          placeholder="选择数据库"
          style="width: 180px"
          :loading="page.loadingMeta"
          @change="page.loadTables"
        >
          <el-option v-for="item in page.databases" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="page.selectedTable" placeholder="选择数据表" style="width: 180px">
          <el-option v-for="item in page.tables" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" :loading="page.loadingPreview" @click="page.loadBrowseData">
          加载数据
        </el-button>
        <span class="hint">默认展示 10 条</span>
      </div>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>数据预览</template>
            <el-table :data="page.previewRows" size="small" border v-loading="page.loadingPreview" height="320">
              <el-table-column
                v-for="col in page.previewColumns"
                :key="col"
                :prop="col"
                :label="col"
                min-width="120"
              />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <template #header>表结构信息</template>
            <el-table :data="page.schemaRows" size="small" border v-loading="page.loadingPreview" height="320">
              <el-table-column prop="columnName" label="字段" min-width="120" />
              <el-table-column prop="dataType" label="类型" min-width="100" />
              <el-table-column prop="columnDesc" label="说明" min-width="120" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <template #header>指标信息</template>
            <el-table :data="page.metricRows" size="small" border v-loading="page.loadingPreview" height="320">
              <el-table-column prop="metricCode" label="编码" min-width="120" />
              <el-table-column prop="metricName" label="名称" min-width="120" />
              <el-table-column prop="description" label="定义" min-width="160" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-title">自然语言查询</div>
      </template>
      <el-input
        v-model="page.question"
        type="textarea"
        :rows="3"
        placeholder="例如：最近7天GMV是多少"
      />
      <div class="query-actions">
        <el-button type="primary" :loading="page.loadingQuery" @click="page.runNaturalQuery">查询</el-button>
        <el-button @click="page.clearQuery">清空</el-button>
      </div>

      <el-divider />

      <el-card shadow="never" class="result-card">
        <template #header>查询结果数据</template>
        <el-table
          v-if="page.resultRows.length"
          :data="page.resultRows"
          size="small"
          border
          v-loading="page.loadingQuery"
        >
          <el-table-column
            v-for="col in page.resultColumns"
            :key="col"
            :prop="col"
            :label="col"
            min-width="140"
          />
        </el-table>
        <el-empty v-else description="暂无查询结果，请先输入问题并点击查询" />
      </el-card>

      <el-card shadow="never" class="result-card">
        <template #header>
          <div class="sql-header">
            <span>查询 SQL</span>
            <el-button link type="primary" @click="copySql">复制</el-button>
          </div>
        </template>
        <el-input type="textarea" :rows="5" :model-value="page.resultSql" readonly />
      </el-card>

      <el-card shadow="never" class="result-card">
        <template #header>查询逻辑解释</template>
        <div class="explanation">{{ page.resultExplanation || '暂无解释' }}</div>
      </el-card>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px;
}
.card {
  margin-bottom: 14px;
}
.card-title {
  font-weight: 600;
}
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.hint {
  color: #909399;
  font-size: 13px;
}
.query-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
.result-card {
  margin-bottom: 10px;
}
.sql-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.explanation {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>

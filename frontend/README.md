# Smart BI Assistant Frontend

基于 Vue3 + TypeScript + Vite + Element Plus 的前端项目（MVP 联调用）。

## 1. 页面功能概览

当前主页面：`src/pages/QueryPage.vue`

包含两大功能区：

1) 数据浏览（上半区）
- 数据库下拉
- 数据表下拉
- 加载数据按钮（默认展示 10 条）
- 同屏展示三类信息：
  - 数据预览（查询结果表格）
  - 表结构信息（字段列表）
  - 指标信息（metric_definition）

2) 自然语言取数（下半区）
- 输入自然语言问题
- 查询按钮
- 返回结果三块展示：
  - 结果数据（data）
  - 查询 SQL（sql）
  - 查询逻辑解释（explanation）

## 2. 本地运行

前置条件：
- 后端已启动并监听 `8080`（`mvn spring-boot:run`）

启动前端：

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：
- `http://localhost:5173/`

## 3. 与后端联调

### API 代理

当前 Vite 配置将 `'/api'` 代理到后端：
- `'/api' -> http://localhost:8080`

因此前端请求建议以 `/api/...` 作为路径（代码里已按此设计）。

### 后端元数据接口（用于数据浏览区）
- `GET /api/metadata/databases`
- `GET /api/metadata/tables?database=...`
- `GET /api/metadata/table-schema?database=...&table=...`
- `GET /api/metadata/metrics`
- `GET /api/metadata/preview?database=...&table=...&limit=10`

### 自然语言查询接口（用于下半区）
- `POST /api/query`

请求体示例：
```json
{ "question": "最近7天GMV是多少" }
```

## 4. 环境变量（可选）

当前支持 `frontend/.env.local`：

- `VITE_API_BASE_URL`

说明：
- 若不配置，默认走 Vite 代理。
- 若配置，会对 baseURL 做兼容归一化（最终仍会以 `/api` 形式发请求）。

提供模板：
- `frontend/.env.example`

## 5. 目录结构（简化）

- `src/pages/QueryPage.vue`：页面主装配
- `src/composables/useQueryPage.ts`：页面状态与交互逻辑
- `src/api/*`：接口封装
- `src/types/*`：类型定义

## 6. 排障建议

如果下拉框没有数据，优先检查：
- 后端是否正在监听 `8080`
- 浏览器 Network 中是否请求到 `GET /api/metadata/databases`
- 控制台是否有 CORS 或网络错误提示

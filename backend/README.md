# Smart BI Assistant (Backend MVP)

一个可控的自然语言取数后端服务（MVP 版本），支持：

1. 接收自然语言问题
2. 结合语义层信息构建 Prompt
3. 调用模型生成 SQL
4. SQL 安全校验与归一化
5. 执行 MySQL 查询
6. 调用模型生成解释
7. 返回 `sql + data + explanation`

---

## 1. 技术栈

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- RestClient（调用 MiniMax / DeepSeek）

---

## 2. 项目结构与作用

```text
SmartBIAssistant
├── src/main/java/com/smartbi
│   ├── SmartBiAssistantApplication.java      # 启动入口，加载配置属性
│   ├── controller
│   │   └── QueryController.java              # 对外 API：/api/query
│   ├── service
│   │   ├── QueryService.java                 # 查询编排接口
│   │   └── impl/QueryServiceImpl.java        # 核心流程实现
│   ├── ai
│   │   ├── AiService.java                    # AI 能力抽象
│   │   └── impl/AiServiceImpl.java           # MiniMax/DeepSeek 调用封装
│   ├── sql
│   │   ├── PromptBuilder.java                # Prompt 构建
│   │   ├── SqlValidator.java                 # SQL 安全校验和 LIMIT 约束
│   │   └── SqlExecutor.java                  # SQL 执行与结果转换
│   ├── mapper
│   │   ├── MetricDefinitionMapper.java       # 指标定义表 Mapper
│   │   └── TableSchemaMapper.java            # 表结构元数据 Mapper
│   ├── entity
│   │   ├── MetricDefinition.java             # 指标定义实体
│   │   └── TableSchema.java                  # 表结构元数据实体
│   ├── dto
│   │   ├── QueryRequest.java                 # 查询请求 DTO
│   │   └── QueryResponse.java                # 查询响应 DTO
│   ├── config
│   │   ├── MybatisPlusConfig.java            # Mapper 扫描配置
│   │   ├── AiProperties.java                 # AI 配置映射
│   │   └── QueryProperties.java              # SQL 安全/执行配置映射
│   └── common
│       ├── exception/BusinessException.java  # 业务异常
│       └── handler/GlobalExceptionHandler.java # 全局异常处理
├── src/main/resources
│   ├── application.yml                       # 公共配置（可提交）
│   ├── application-local.yml                 # 本地敏感配置（忽略提交）
│   ├── application-local.example.yml         # 本地配置模板
│   └── schema.sql                            # 语义层元数据表初始化脚本
├── .gitignore                                # 忽略敏感文件和构建产物
└── pom.xml                                   # Maven 依赖管理
```

---

## 3. 核心调用流程

1. 前端调用 `POST /api/query`
2. `QueryServiceImpl` 读取语义层数据（`metric_definition`、`table_schema`）
3. `PromptBuilder` 生成模型 Prompt
4. `AiServiceImpl.generateSql()` 调用 MiniMax 生成 SQL
5. `SqlValidator` 校验并归一化 SQL（只允许 SELECT、强制 LIMIT、白名单控制）
6. `SqlExecutor` 查询 MySQL
7. `AiServiceImpl.explainResult()` 调用 DeepSeek 生成解释
8. 返回 `QueryResponse`

---

## 4. 数据库说明

当前 MVP 使用 MySQL，语义层需要两张表（见 `schema.sql`）：

- `metric_definition`：指标定义
- `table_schema`：表结构元数据

---

## 5. API 说明

### `POST /api/query`

请求体：

```json
{
  "question": "最近7天GMV是多少"
}
```

响应体：

```json
{
  "sql": "SELECT ... LIMIT 1000",
  "data": [],
  "explanation": "最近7天GMV为..."
}
```

---

## 6. 配置与敏感信息管理

### 可提交配置

- `application.yml`：只放公共配置和非敏感默认值

### 不可提交配置

- `application-local.yml`：放本地 MySQL 密码、模型 API Key
- 该文件已加入 `.gitignore`

### 建议使用方式

1. 复制 `application-local.example.yml` 为 `application-local.yml`
2. 填入本地账号密码和 API Key
3. 启动时使用 `local` profile（默认已配置）

---

## 7. 快速启动

1. 创建 MySQL 数据库，例如：`smart_bi`
2. 执行 `src/main/resources/schema.sql`
3. 填写 `application-local.yml` 中的本地配置
4. 启动项目：

```bash
mvn spring-boot:run
```

5. 调用接口联调：

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question":"最近7天GMV是多少"}'
```

---

## 8. 当前 MVP 范围

已包含：

- 核心链路打通
- SQL 安全基础控制
- 语义层读取
- 模型调用抽象

暂未包含：

- StarRocks 执行器
- Redis 缓存
- 查询历史
- 高并发与高可用优化


# Smart BI Assistant (Backend)

一个可控的自然语言取数后端服务，当前版本已支持：

1. 自然语言问题 -> SQL 生成 -> SQL 校验 -> MySQL 查询 -> 结果解释
2. MiniMax 优先、DeepSeek 兜底的双模型容灾
3. SQL 安全约束（只读、LIMIT、白名单、超时）
4. 本地一键初始化数据库与示例数据

---

## 1. 技术栈

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL / MariaDB
- RestClient（MiniMax + DeepSeek）

---

## 2. 项目结构

```text
backend/
├── src/main/java/com/smartbi
│   ├── controller                 # API 入口
│   ├── service                    # 业务编排
│   ├── ai                         # 模型调用封装（含 fallback）
│   ├── sql                        # Prompt、校验、执行
│   ├── mapper                     # MyBatis-Plus Mapper
│   ├── entity                     # 数据实体
│   ├── dto                        # 请求响应对象
│   ├── config                     # 配置绑定与扫描
│   └── common                     # 异常与通用处理
├── src/main/resources
│   ├── application.yml            # 公共配置（可提交）
│   ├── application-local.yml      # 本地敏感配置（忽略提交）
│   ├── application-local.example.yml
│   └── schema.sql                 # 建库建表 + 示例数据
└── pom.xml
```

---

## 3. 核心链路

1. 调用 `POST /api/query`
2. 读取语义层元数据（`metric_definition`、`table_schema`）
3. 构建 Prompt
4. 生成 SQL（MiniMax 优先，失败自动切 DeepSeek）
5. SQL 规范化与安全校验
6. 执行 SQL
7. 生成解释（MiniMax 优先，失败自动切 DeepSeek）
8. 返回 `sql + data + explanation`

---

## 4. 模型调用说明

### MiniMax（Anthropic Compatible）

- 模型：`MiniMax-M2.7`（默认，可改）
- 协议：Anthropic messages
- 默认地址：`https://api.minimaxi.com/anthropic/v1/messages`
- Header：`x-api-key` + `anthropic-version`

### DeepSeek（Chat Completions）

- 模型：`deepseek-chat`（默认，可改）
- base-url 配置为：`https://api.deepseek.com`
- 代码会自动拼接：`/chat/completions`

### Fallback 策略

- SQL 生成：MiniMax -> DeepSeek
- 结果解释：MiniMax -> DeepSeek
- 日志会打印最终使用的模型

---

## 5. SQL 安全规则

- 仅允许 `SELECT` 与 `WITH ... SELECT ...`（CTE）
- 禁止写操作关键字（`UPDATE/DELETE/INSERT/DROP/ALTER/TRUNCATE`）
- 必须带 `LIMIT`（自动补齐并限制最大值）
- 可配置表白名单
- 查询超时控制（默认 10 秒）

---

## 6. 数据库初始化

执行 `src/main/resources/schema.sql` 后会自动完成：

- 创建数据库：`smart_bi`
- 创建表：`metric_definition`、`table_schema`、`orders`
- 初始化语义元数据
- 初始化示例订单数据（可重复执行）

---

## 7. 配置说明

### 公共配置（可提交）

- 文件：`src/main/resources/application.yml`
- 放非敏感默认值（端口、模型名、默认地址等）

### 本地配置（不可提交）

- 文件：`src/main/resources/application-local.yml`
- 放数据库密码、API Key 等敏感信息
- 已被 `.gitignore` 忽略

---

## 8. 启动与测试

1. 准备本地配置：编辑 `application-local.yml`
2. 初始化数据库：执行 `schema.sql`
3. 启动：

```bash
mvn spring-boot:run
```

4. 调用测试：

```bash
curl -X POST "http://localhost:8080/api/query" \
  -H "Content-Type: application/json" \
  -d '{"question":"最近7天GMV是多少"}'
```

---

## 9. 调试日志

当前已增加关键阶段日志：

- API 入口调用
- 语义层读取
- Prompt 构建
- MiniMax/DeepSeek 请求与模型名
- fallback 触发路径
- SQL 执行行数与异常定位信息


# distributed-base · 分布式基础工程脚手架

> **定位**：打通核心技术栈的开箱即用基础框架，供团队分工开发业务逻辑。代码均可编译运行，无伪代码。

---

## 一、技术栈总览

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **后端框架** | Spring Boot | 3.2.x | 主框架，基于 Jakarta EE 10 |
| **文档数据库** | MongoDB | 7.x | 非结构化日志存储 |
| **缓存 / 计数** | Redis | 7.2 | 缓存 + 原子计数器 |
| **WebService** | Apache CXF | 4.0.x | SOAP/JAX-WS 标准 |
| **构建工具** | Maven | 3.9+ | 多 Profile 环境隔离 |
| **JDK** | Java | 17 LTS / 21 LTS | 弹性切换 |
| **前端框架** | UniApp + Vue 3 | 3.x | H5 + 微信小程序双端 |
| **构建工具** | Vite | 5.x | 前端开发服务器与打包 |
| **类型系统** | TypeScript | 5.x | 全量类型安全 |
| **反向代理** | Nginx | 1.25 | 负载均衡 + 跨域 |
| **容器编排** | Docker Compose | v2 | 基础设施一键拉起 |

---

## 二、工程目录结构与文件说明

```
distributed-base/
│
├── backend/                          # 后端 Spring Boot 工程
│   ├── pom.xml                       # Maven 配置：JDK 弹性、全依赖管理
│   └── src/main/
│       ├── resources/
│       │   ├── application.yml       # 公共/生产配置（Jackson、CXF路径、日志格式）
│       │   └── application-dev.yml   # 本地开发配置（MongoDB/Redis 地址端口）
│       └── java/com/distributed/base/
│           ├── DistributedBaseApplication.java   # Spring Boot 启动类
│           ├── config/
│           │   ├── RedisConfig.java              # Redis 序列化防乱码配置
│           │   ├── CxfWebServiceConfig.java      # CXF 端点发布（/ws/SystemIntegrationService）
│           │   └── CorsConfig.java               # 全局 CORS 跨域配置
│           ├── entity/
│           │   └── SystemLog.java               # MongoDB 文档实体（@Document + 复合索引）
│           ├── repository/
│           │   └── SystemLogRepository.java     # MongoRepository 数据访问层
│           ├── service/
│           │   └── SystemService.java           # 核心业务层（Mongo + Redis 统一入口）
│           ├── webservice/
│           │   ├── SystemIntegrationWs.java     # SOAP 契约接口（JAX-WS @WebService）
│           │   └── impl/
│           │       └── SystemIntegrationWsImpl.java  # SOAP 实现（注入 SystemService）
│           ├── controller/
│           │   └── ApiController.java           # REST 联调接口（Mongo/Redis/统计）
│           └── exception/
│               └── GlobalExceptionHandler.java  # 全局异常处理器
│
├── frontend/                         # 前端 UniApp 工程
│   ├── package.json                  # 依赖声明（Node 18~22 兼容范围）
│   ├── vite.config.ts                # Vite 配置（开发代理 /api → 8080）
│   ├── tsconfig.json                 # TypeScript 配置（严格模式 + 路径别名）
│   ├── index.html                    # H5 端入口 HTML
│   └── src/
│       ├── main.ts                   # 应用入口（createSSRApp 兼容小程序）
│       ├── App.vue                   # 根组件（全局样式重置）
│       ├── pages.json                # UniApp 路由配置
│       ├── utils/
│       │   └── request.ts            # 统一请求库（H5/小程序 API 抹平 + Token + 拦截器）
│       ├── api/
│       │   └── system.ts            # API 集中调用层（类型安全 + 接口 1:1 映射）
│       └── pages/
│           └── index/
│               └── index.vue        # 联调看板页面（全功能可视化测试面板）
│
├── nginx/
│   └── nginx.conf                   # 反向代理 + 负载均衡 + CORS + Gzip
│
├── redis/
│   └── redis.conf                   # Redis 本地配置（AOF+RDB 持久化，限制 256MB）
│
├── mongo-init/
│   └── init.js                      # MongoDB 容器初始化脚本（建库/索引/初始文档）
│
└── docker-compose.yml               # 基础设施一键编排（MongoDB + Redis + Nginx）
```

---

## 三、前后端关联链路

```
前端 UniApp (H5 / 小程序)
    │
    ├── src/utils/request.ts      ← 统一 HTTP 客户端（抹平端差异）
    │       ↓
    ├── src/api/system.ts         ← API 集中声明层
    │       ↓
    │   [H5 端走 Vite 代理 → Nginx 80 → backend_pool]
    │   [小程序端直连 HTTPS → Nginx 80 → backend_pool]
    │
Nginx (80 端口)
    ├── /api/*  → upstream backend_pool (8080 / 8081) 轮询
    ├── /ws/*   → upstream backend_pool (SOAP 透传)
    └── /       → 静态资源 (frontend/dist)
    │
Spring Boot 后端 (8080 / 8081)
    ├── ApiController (/api/*)
    │       ↓
    ├── SystemService             ← 业务逻辑唯一入口
    │   ├── MongoRepository       → MongoDB 7 (27017)
    │   └── RedisTemplate         → Redis 7 (6379)
    │
    └── CXF WebService (/ws/SystemIntegrationService)
            ↓
        SystemIntegrationWsImpl
            ↓
        SystemService             ← 同一 Bean（SOAP/REST 数据一致）
```

---

## 四、快速启动指南

### 1. 拉起基础设施（Docker）

```bash
# 在项目根目录执行
docker compose up -d

# 验证容器状态
docker compose ps
```

> MongoDB: `localhost:27017` · Redis: `localhost:6379` · Nginx: `localhost:80`

### 2. 启动后端（本地 IDE 或 Maven）

```bash
cd backend

# JDK 17（默认）
mvn spring-boot:run -Pdev

# JDK 21（本地装了 21 时）
mvn spring-boot:run -Pjdk21,dev
```

访问验证：
- REST 健康探针：`http://localhost:8080/api/ping`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- WSDL：`http://localhost:8080/ws/SystemIntegrationService?wsdl`

### 3. 启动前端（H5 开发模式）

```bash
cd frontend
npm install
npm run dev:h5
```

浏览器访问：`http://localhost:5173` — 联调看板页面

### 4. 微信小程序模式

```bash
npm run dev:mp-weixin
# 使用微信开发者工具打开 dist/dev/mp-weixin 目录
```

---

## 五、JDK 版本兼容说明

| 场景 | 命令 |
|------|------|
| 本地装了 JDK 17 | `mvn spring-boot:run -Pdev`（默认，无需特殊参数）|
| 本地装了 JDK 21 | `mvn spring-boot:run -Pjdk21,dev`（自动激活 JDK 21 Profile）|
| 打包生产 JAR | `mvn package -Pprod -DskipTests` |

> Maven 通过 `<activation><jdk>` 自动探测本地 JDK 版本激活对应 Profile，成员无需手动修改 pom.xml。

---

## 六、多环境配置说明

| 文件 | 用途 | 是否提交版本库 |
|------|------|----------------|
| `application.yml` | 生产/公共配置 | ✅ 提交 |
| `application-dev.yml` | 本地开发配置（IP/端口） | ⚠️ 按需（建议 .gitignore）|

本地成员只需修改 `application-dev.yml` 中的 `host` 字段，切换到自己的 MongoDB/Redis 地址，互不影响。

---

## 七、WebService 调用示例

使用 SoapUI / Apifox 发送以下 SOAP 请求：

**端点**：`http://localhost:8080/ws/SystemIntegrationService`

**ping 请求**：
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:web="http://webservice.base.distributed.com/">
  <soapenv:Header/>
  <soapenv:Body>
    <web:ping/>
  </soapenv:Body>
</soapenv:Envelope>
```

**writeLog 请求**：
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:web="http://webservice.base.distributed.com/">
  <soapenv:Header/>
  <soapenv:Body>
    <web:writeLog>
      <module>TEST</module>
      <action>CREATE</action>
      <operator>soap-client</operator>
      <content>SOAP 联调测试写入</content>
    </web:writeLog>
  </soapenv:Body>
</soapenv:Envelope>
```

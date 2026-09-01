# 第二手 SecondHand AI — 后端服务

校园二手交易平台（AI 版）后端，Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + PostgreSQL 16。

> 本仓库当前为**田博（后端）**负责模块的代码：数据库设计、商品核心业务接口、AI 估价规则引擎、AI 自动填表发布链路。
> 登录/权限、AiService 大模型 API（陈思瀚）、私信/减碳（林天楚）等模块将逐步并入。

---

## 1. 技术栈与版本

| 组件 | 版本 |
|------|------|
| JDK | 17（本机路径 `C:\Users\16839\.jdks\ms-17.0.20.1`） |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.7 |
| PostgreSQL | 15/16（库名 `second_hand`） |
| Maven | 3.9.16（本机路径见下） |

## 2. 目录结构

```
├── pom.xml                    # 生产 POM（联网构建）
├── pom.verify.xml             # 离线验证 POM（本机无网编译验证用，勿部署）
├── settings.offline.xml       # 离线镜像 settings（映射到项目内 maven-repo）
├── maven-repo/                # 项目内 Maven 本地仓库（离线构建依赖缓存）
├── sql/schema.sql             # 全库建表脚本（14 张表 + 种子数据）
├── docs/
│   ├── 任务计划书-田博.md       # 任务计划书（进度追踪）
│   ├── 数据库设计文档.md        # 表结构设计说明
│   └── 接口说明文档.md          # 全部接口文档
└── src/
    ├── main/java/com/hdu/secondhand/
    │   ├── common/            # Result 统一返回 / 异常体系 / 状态常量
    │   ├── config/            # CORS / MyBatis-Plus 分页插件
    │   ├── controller/        # 商品、收藏、AI 估价、AI 发布接口
    │   ├── service/           # 业务接口 + 实现
    │   ├── mapper/            # MyBatis-Plus Mapper（含推荐查询 XML）
    │   ├── entity/            # 实体（与表一一对应）
    │   ├── dto/ vo/           # 入参 / 出参
    │   ├── ai/                # AiService 封装（接口 + Mock + HTTP 骨架）
    │   ├── ai/rules/          # ★ AI 估价规则引擎（纯 Java，零依赖）
    │   └── util/UserContext   # 当前用户解析（X-User-Id 头）
    └── test/java/             # 单元测试（规则引擎 / 商品服务 / AI 发布链路）
```

## 3. 快速开始（联网环境，团队标准流程）

```bash
# 1. 初始化数据库（PostgreSQL 15/16）
psql -U postgres -f sql/schema.sql

# 2. 修改数据库密码
#    src/main/resources/application.yml → spring.datasource.password

# 3. 构建并运行（首次会自动拉取依赖，建议配置阿里云镜像）
mvn clean package
java -jar target/second-hand-backend-0.1.0-SNAPSHOT.jar

# 4. 接口根路径 http://localhost:8080/api/...
```

## 4. 本机离线编译与测试（无网络环境）

本机无外网且 `~/.m2` 元数据指向不可用镜像，因此：

```bash
# 编译主代码（使用项目内 maven-repo + file:// 镜像，不联网）
mvn -s settings.offline.xml -f pom.verify.xml clean compile

# 编译测试代码
mvn -s settings.offline.xml -f pom.verify.xml test-compile

# 手动运行单元测试（本机仓库缺 surefire-junit-platform / junit-platform-launcher，
# 无法 mvn test；使用内置 TestRunner，行为与 JUnit5 一致）
java -cp <test-classpath> com.hdu.secondhand.TestRunner
```

> ⚠️ `pom.verify.xml` 与 `settings.offline.xml` 仅供本机离线验证，**部署请使用 `pom.xml` 并在联网环境构建**。

## 5. 接口一览（已对齐《接口约定规范 v1.0》）

统一返回 `{code, message, data}`（**code=0 成功**），金额单位**分**，分页 `{list,total,page,pageSize}`。

| 模块 | 方法/路径 | 说明 |
|------|-----------|------|
| 商品 | POST `/api/products` | 发布商品 |
| 商品 | PUT `/api/products/{id}` | 编辑（仅本人） |
| 商品 | PUT `/api/products/{id}/status` | 上架/下架 |
| 商品 | DELETE `/api/products/{id}` | 删除（逻辑） |
| 商品 | GET `/api/products` | 分页浏览/检索（免登录只读） |
| 商品 | GET `/api/products/{id}` | 详情（免登录只读） |
| 商品 | GET `/api/products/mine` | 我的商品 |
| 商品 | GET `/api/products/recommend` | 猜你喜欢 |
| 商品 | GET `/api/products/history` | 浏览足迹 |
| 收藏 | POST `/api/favorites/{productId}` | 收藏/取消（幂等切换） |
| 收藏 | GET `/api/favorites` | 我的收藏 |
| AI | POST `/api/ai/identify` | AI 识别（图片→分类+成色，base64） |
| AI | POST `/api/ai/describe` | AI 描述生成 |
| AI | POST `/api/ai/estimate` | AI 智能估价（双层+降级，金额分） |
| AI | POST `/api/ai/draft` | AI 自动填表（草稿） |
| AI | POST `/api/ai/publish` | AI 一键发布 |
| 管理 | PUT `/api/admin/products/{id}/review` | 商品审核流转（通过→在售/驳回→审核驳回） |
| 字典 | GET `/api/dicts` | 枚举字典（免登录） |

`/api/ai/chat`（AI 问答，**林天楚**）、`/api/ai/review`（AI 预检，**陈思瀚**）、`/api/ai/recommend`（AI 推荐位，**陈思瀚**）为其他成员模块。详见 `docs/接口说明文档.md`。

## 6. 与其他成员的集成约定

| 约定 | 说明 |
|------|------|
| 统一响应 | `Result<T>{code,message,data}`，**code=0 成功**；分页 `PageResult{list,total,page,pageSize}`；错误码/HTTP 状态对齐规范 v1.0 |
| 金额单位 | 接口层整数「分」（`util/MoneyUtil` 与数据库元互转） |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`（`config/JacksonConfig` 全局） |
| 当前用户 | 优先 `Authorization: Bearer <JWT>`（`util/JwtTokenService`，陈思瀚接入）；开发期兼容 `X-User-Id` 头 |
| AiService | 接口 `ai/AiService`：`ai.mock=true`（默认）走 `MockAiService`（离线，engine=rule）；`ai.mock=false`+`ai.enabled=true` 走真实模型（engine=llm，失败自动降级），开关由组长统一控制 |
| 枚举 | `ai/CategoryEnum` 维护规范分类 key（book/digital/...）与数据库 ID 映射；成色 100/90/80/70 ↔ 1~10 |
| 商品审核 | 审核流程：AI 预检 `/api/ai/review`（陈思瀚）→ 管理员决定 → `PUT /api/admin/products/{id}/review`（田博，状态流转+驳回原因） |
| CORS | 已开放（`application.yml → cors.allowed-origins`） |

## 7. 里程碑对照

- ✅ V0.1：数据库表结构（`sql/schema.sql`）、后端工程骨架、商品基础接口
- ✅ V0.5：商品核心接口全集、AI 估价规则引擎、AI 自动填表发布链路、单元测试
- ⬜ V1.0：担保交易/信用分/减碳接口联调（表结构已就绪，接口待对应成员/后续开发）

## 8. 已知说明

- 本机无 PostgreSQL 驱动缓存且无网络：`pom.xml` 中的 `org.postgresql:postgresql` 需联网下载；离线验证 POM 未引入（编译期不引用其类）。
- 中文检索：`product` 表已建 `pg_trgm` GIN 索引，业务层同时提供 LIKE 兜底。

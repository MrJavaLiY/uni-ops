# UniOps 统一运维平台

UniOps 是一个基于 Spring Boot 3.x 开发的统一运维平台，提供通用的实体管理和HTTP调用日志记录等功能。

## 技术栈

- **后端框架**: Spring Boot 3.2.0
- **编程语言**: Java 21
- **持久层**: MyBatis-Plus 3.5.5
- **数据库**: 支持 H2（开发测试）、SQL Server 2019
- **连接池**: Druid
- **HTTP客户端**: OkHttp 4.12.0
- **JSON处理**: Fastjson2
- **API文档**: SpringDoc + Knife4j
- **AOP**: AspectJ

## 功能特性

### 1. 通用实体管理
- 支持动态实体的CRUD操作
- 自动识别实体主键字段
- 支持分页查询（兼容SQL Server分页语法）
- 实体缓存管理
- 自动类型转换（支持Long、Integer、Date等常见类型）

### 2. HTTP请求日志记录
- 拦截所有进入的HTTP请求
- 记录请求参数、响应消息、耗时等信息
- 记录异常栈信息
- 支持日志清理策略

### 3. 第三方HTTP调用日志
- 记录对外部服务的HTTP调用
- 包含请求地址、参数、响应等完整信息
- 支持多种HTTP方法（GET、POST、PUT、DELETE、PATCH等）
- 提供OkHttp工具类进行HTTP调用

### 4. 任务调度
- 集成Quartz任务调度框架
- 支持动态任务管理
- 提供任务执行日志




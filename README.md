# 精灵宝贝对战助手：Java 后端

Java 21、Spring Boot、Spring MVC、Maven Wrapper、Spring JDBC 和 MySQL。

## 第一次配置 MySQL

安装并启动本机 MySQL 8.4 LTS。在 MySQL 客户端用管理员账号执行以下 SQL；先把“换成你的应用密码”替换为自选密码。应用使用单独账号。

```sql
CREATE DATABASE IF NOT EXISTS battle_assistant CHARACTER SET utf8mb4;
CREATE USER 'battle_assistant'@'127.0.0.1' IDENTIFIED BY '换成你的应用密码';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, REFERENCES
    ON battle_assistant.* TO 'battle_assistant'@'127.0.0.1';
```

已创建账号时跳过 CREATE USER。库由这一步建立；应用连上后通过 `schema.sql` 创建 `elves`、`elf_types` 和 `catalog_entries` 表。

## 启动与测试

在 Cursor 打开上一级 project 工作区。停止旧实例后，在 **Run and Debug** 选择 **游戏助手后端（8080）**，启动时输入刚设置的应用账号密码。启动配置只保存密码输入框，不保存密码。

终端启动时，从 backend 目录向进程提供环境变量 `BATTLE_DB_PASSWORD`，再运行 `./mvnw spring-boot:run`。默认连接 `127.0.0.1:3306/battle_assistant`，默认用户为 `battle_assistant`；可用 `BATTLE_DB_URL` 和 `BATTLE_DB_USER` 覆盖。不要将密码写进 Git。HTTP 服务仍在 http://localhost:8080，根路径没有首页。

接口集成测试使用独立的 MySQL 测试库。先按上面的建库和授权步骤建立空的 `battle_assistant_test` 库及同名账号，授权范围仅限测试库。向运行 Maven 的进程设置 `BATTLE_TEST_MYSQL=1` 和 `BATTLE_TEST_DB_PASSWORD`，再执行 `./mvnw test`。测试事务结束时自动回滚写入；默认跳过数据库测试。

## 接口文档与队伍配置

完整字段、状态码和示例见 [接口文档](docs/API.md)。队伍录入已实现，使用 [REST Client 队伍示例](requests-teams.http) 依次创建测试资料和队伍。

重启应用时会新增 teams、team_members、team_member_skills 三张表。原有资料不需清空。队伍保存的是实际配招、特性、携带道具、等级和能力值，允许先存草稿；不代表游戏模式合法性或预测能力。

## 资料接口

| 路径 | 资料 | 请求字段 |
| --- | --- | --- |
| /api/elves | 精灵 | name、types（属性名称数组） |
| /api/skills | 技能 | name、description；可选 category、element、power、accuracy（0–100）、priority、pp |
| /api/traits | 特性 | name、description；可选 category |
| /api/items | 道具、树果 | name、description；可选 category，例如“树果” |

每类支持 `GET /集合` 列表、`POST /集合` 新增、`GET /集合/{id}` 查询、`PUT /集合/{id}` 整条替换和 `DELETE /集合/{id}` 删除。新增返回 201 和 Location；删除成功返回 204；不存在的 ID 返回 404。必填字段不合法返回 400。技能、特性和道具的响应将资料字段放在 `details` 中，写入时直接发送这些字段。

以下是测试资料，不代表真实游戏规则：

```sh
curl -i http://localhost:8080/api/elves \
  -H 'Content-Type: application/json' \
  -d '{"name":"测试精灵","types":["水"]}'

curl -i http://localhost:8080/api/skills \
  -H 'Content-Type: application/json' \
  -d '{"name":"测试技能","description":"示例效果","element":"水"}'
```

浏览器可直接打开 http://localhost:8080/api/elves 查看列表。

## 分层

`Controller → Service → Repository → MySQL`：Controller 处理 HTTP，Service 校验和整理资料，Repository 用 SQL 读写数据库。应用配置在 `application.yaml`，表结构在 `schema.sql`。Java 的 record 用来表达请求字段和查询结果。

这版完成资料录入与存储。未预置未经核实的游戏数值；队伍配招关联已实现；精灵通用技能池、对战规则计算和推荐接口仍属于后续阶段。每条精灵属性都在 elf_types 中占一行，用 elf_id 外键关联精灵；修改精灵和属性在同一事务中执行。

## 赛前截图识别（线上服务基础版本）

新增 `/preparation.html` 测试及素材录入页面，支持截图区域校准、12 个头像下载、身份／热门特性录入、阵容识别和首发标记。新增一张 `preparation_avatars` 表；头像通过 OSS HTTPS 地址预加载到服务端内存，识别请求不访问对象存储。无需 Mac OCR，也未新增 OpenCV 原生依赖。

完整阿里云 OSS 配置、录入、部署与验收步骤见 [赛前识别指南](docs/PREPARATION.md)。当前为待真实素材校准的匹配基础版本，尚未部署或验证准确率／十秒时限，尚不生成出招建议。

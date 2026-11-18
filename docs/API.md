# 精灵宝贝对战助手后端接口文档

## 1. 范围与约定

基础地址：`http://localhost:8080`。接口仅覆盖资料管理和队伍配置，尚无实时画面识别、对战模拟、出招推荐和用户登录功能。适用于本机开发，不应直接公开为多人服务。

- 请求体使用 `Content-Type: application/json`。
- GET 返回 200；POST 新增返回 201 和相对路径 `Location`；PUT 返回 200；DELETE 返回 204，无响应体。
- PUT 是整条替换，不是局部修改。ID 由数据库生成，不应假定从 1 开始或连续。
- 列表直接返回数组，空列表为 `[]`。暂不支持分页、搜索和排序参数。
- 未找到记录返回 404；输入校验失败返回 400；数据约束冲突返回 409；数据库操作不可用返回 503。
- 未知游戏字段保留 null，不默认猜测。数据库未预置真实精灵或规则；例子均为测试资料。
- 同名资料允许存在。条目类型由路径决定，PUT 不能将技能变成道具。
- 游戏合法性（是否能学某技能、队伍人数、实际等级上限等）还未校验。

## 2. 接口总览

| 资源 | GET 列表 / POST 新增 | GET 查询 / PUT 替换 / DELETE 删除 |
| --- | --- | --- |
| 精灵资料 | /api/elves | /api/elves/{id} |
| 技能资料 | /api/skills | /api/skills/{id} |
| 特性资料 | /api/traits | /api/traits/{id} |
| 道具（含树果） | /api/items | /api/items/{id} |
| 我的队伍 | /api/teams | /api/teams/{id} |

`GET /health` 是最初的教学接口，当前沿用工作区中的 `{"status":"no"}`；它仅验证 Web 响应，不表示系统不健康，也不检查数据库连接。

## 3. 精灵资料

### POST /api/elves 与 PUT /api/elves/{id}

| 字段 | 类型 | 必填 | 校验 |
| --- | --- | --- | --- |
| name | string | 是 | 去首尾空白后 1–120 字符 |
| types | string[] | 是 | 至少一个属性，每项非空且最多 60 字符；去首尾空白、去重 |

```json
{"name":"测试精灵","types":["水","冰"]}
```

新增响应示例（201，`Location: /api/elves/12`）：

```json
{"id":12,"name":"测试精灵","types":["水","冰"]}
```

GET 单条返回同样对象，GET 列表返回这些对象的数组。删除被队伍引用的精灵返回 409；未引用时，删除精灵会连同其属性行一起删除。

## 4. 技能、特性、道具资料

写入请求字段放在 JSON 顶层；查询响应将资料放在 `details` 中。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 去首尾空白后 1–120 字符 |
| description | string | 是 | 去首尾空白后 1–4000 字符，保存效果原文 |
| category | string/null | 否 | 最多 60 字符；道具可用“树果”等分类 |
| element | string/null | 否 | 仅技能使用，最多 60 字符 |
| power | integer/null | 否 | 仅技能使用，非负 |
| accuracy | integer/null | 否 | 仅技能使用，0–100；未知/非数值命中机制留空 |
| priority | integer/null | 否 | 仅技能使用，可以为负 |
| pp | integer/null | 否 | 仅技能使用，非负 |

空白可选文本会转换为 null。PUT 中省略可选字段会清空原值。

### POST /api/skills 请求

```json
{
  "name":"测试技能",
  "description":"仅用于验证接口的效果说明",
  "category":"物理",
  "element":"水",
  "power":50,
  "accuracy":100,
  "priority":0,
  "pp":10
}
```

响应（201，示例 ID 为 21）：

```json
{
  "id":21,
  "kind":"skills",
  "details":{
    "name":"测试技能",
    "description":"仅用于验证接口的效果说明",
    "category":"物理",
    "element":"水",
    "power":50,
    "accuracy":100,
    "priority":0,
    "pp":10
  }
}
```

### POST /api/traits 请求

```json
{"name":"测试特性","description":"测试效果说明"}
```

### POST /api/items 请求

```json
{"name":"测试树果","description":"测试效果说明","category":"树果"}
```

特性和道具响应结构与技能相同，kind 分别为 traits、items，未提供的可选字段为 null。不同类型共用一个 ID 序列；即使 ID 存在，访问错误的类型路径也返回 404。被队伍引用的资料不能删除，返回 409。

## 5. 我的队伍

### POST /api/teams 与 PUT /api/teams/{id}

保存“这只精灵在我的队伍里实际带什么”，不是精灵通用技能池。现阶段仅保存资料，草稿不代表已足够用于推荐。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 去首尾空白后 1–120 字符 |
| members | object[] | 是 | 允许空数组作为草稿，最多 100 项技术上限 |

每名成员：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| elfId | integer | 是 | 必须引用已存在的精灵资料 |
| nickname | string/null | 否 | 去首尾空白，最多 120 字符 |
| level | integer/null | 否 | 大于 0，未知留空 |
| traitId | integer/null | 否 | 引用已存在的 traits 条目 |
| itemId | integer/null | 否 | 引用已存在的 items 条目，树果也用此字段 |
| skillIds | integer[]/null | 否 | 按配招顺序排列，最多 16 项技术上限，不重复，每项引用 skills；省略视为空数组 |
| stats | object/null | 否 | 实际能力值，字段见下方；未知字段可以留空 |

stats 的字段为 `hp`（最大生命值）、`attack`、`defense`、`specialAttack`、`specialDefense`、`speed`。已填写值必须是正整数；不是当前战斗血量，也不是种族值。

例子中的 ID 必须替换成自己已创建的资料 ID：

```json
{
  "name":"我的第一队",
  "members":[{
    "elfId":12,
    "nickname":"主力",
    "level":50,
    "traitId":22,
    "itemId":23,
    "skillIds":[21],
    "stats":{
      "hp":200,
      "attack":100,
      "defense":80,
      "specialAttack":90,
      "specialDefense":85,
      "speed":110
    }
  }]
}
```

响应对象在上述请求字段基础上增加顶层 id；返回 members 数组保留顺序。成员不暴露独立 ID。省略 stats 时返回六个 null 的 stats 对象。

- 空队伍可先用 `{"name":"队伍草稿","members":[]}` 创建。
- PUT 替换整队，包括成员、配招和能力值；省略成员不是保留旧成员。
- 成员引用不存在或资料类型不符返回 400。
- 写入整队使用事务，中途失败不会留下半支队伍。
- DELETE 删除队伍时级联删除其成员及配招关系，不删除精灵、技能和道具的基础资料。
- 删除成员：读取队伍后，从 members 中移除该成员，PUT 整队。
- 更新资料本身会影响引用它的所有队伍；当前没有版本快照。
- 不保存每场战斗的当前血量、异常状态或已消耗道具，这些属于后续战况模块。

## 6. 错误示例

以下接口错误使用 ProblemDetail，不会返回原始 SQL 或数据库密码：

```json
{
  "type":"about:blank",
  "title":"Bad Request",
  "status":400,
  "detail":"成员引用的精灵不存在"
}
```

`instance` 等字段由运行时补充，可能出现。无效 JSON、路径参数类型不匹配返回 400。无匹配路由返回 404，不支持的方法返回 405，这些由 Spring 处理，其响应格式可能与业务错误不同。

## 7. 启用和验证

1. 在 Cursor 停止旧后端，再用“游戏助手后端（8080）”启动并输入应用账号密码。
2. 原有库不需删除；schema.sql 用 CREATE TABLE IF NOT EXISTS 增加 teams、team_members、team_member_skills。
3. 应用账号需有项目库的 CREATE、REFERENCES 以及增删改查权限。
4. 使用 requests-teams.http 逐个执行示例，先基础资料，再队伍。
5. 单元/HTTP 测试用 ./mvnw test；MySQL 集成测试需独立空测试库、BATTLE_TEST_MYSQL=1 和 BATTLE_TEST_DB_PASSWORD，不能用开发库代替。
6. 本次不需要数据库改名或清空原有资料。后续修改已存在的表结构仍需专门迁移，CREATE TABLE IF NOT EXISTS 不会自动升级旧字段。

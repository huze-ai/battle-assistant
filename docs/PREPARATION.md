# 赛前阵容识别：实施与阿里云 OSS 配置

本模块部署在 Java 后端，用户浏览器上传截图，服务器输出双方各 6 只精灵、形态、默认热门特性及首发。性别、等级、胜率、倒计时不提取。首发固定取各自左上角。热门特性为推定，不代表从图中直接读到。

## 已实现与边界

- 一张新表 `preparation_avatars`，头像保存在 OSS，表中只保存 Key 与资料。与项目现有精灵／队伍表互不影响。
- 服务端固定布局裁剪与轻量颜色特征匹配，不依赖 macOS、外部 AI 服务或 OpenCV 原生库。第一版用 Java ImageIO 实现，后续可替换匹配器。
- 启动后及每 60 秒预加载；图片内容仅在服务器内存中计算为特征。识别请求不读数据库、不访问 OSS、不落盘截图。
- 资料更新整体成功后原子替换缓存；失败保留上一版，状态接口显示错误。多实例各自预加载。
- 未变化的记录复用特征，避免每分钟重复下载。更新图片需更换 Key，例如 `avatars/elf-a-v2.png`；只有 Key 相同但原图更换时不会自动刷新。
- 相似度不足／候选接近返回 unknown，无识别身份；分数不是概率。阈值 0.90、差值 0.025 是初始参数，必须用独立截图校准。
- 尚无真实身份和热门特性种子数据、对战推荐器、线上部署及压测结果。页面只返回阵容；不输出未经规则验证的出招建议。
- 仅适配用户提供的赛前双 2×3 阵容布局。支持校准游戏内容矩形，其他 UI 布局需新增配置。

## 1. 创建 OSS Bucket

打开 [OSS 控制台](https://oss.console.aliyun.com/)，若未开通，按页面提示开通。进入 Bucket 列表并创建 Bucket：

1. 名称使用全局唯一英文名，例如 `你的前缀-battle-avatars`。
2. 地域选计划部署后端的地域。尚未确定服务器地域时先确定地域，减少跨地域访问。
3. 存储类型选择标准存储，其他先保留默认。
4. 本版通过 HTTPS 读取公开头像，无需 AccessKey。请使用只放可公开游戏头像的独立 Bucket。
5. 创建后，在权限控制中关闭该 Bucket 的“阻止公共访问”，将 Bucket ACL 改为“公共读”；写入仍由你的账号控制。

这一步允许任何人读取头像，不适合私人截图。赛前整图无需上传 OSS。若希望头像也私有，保持默认私有，先不要改 ACL；当前读取器尚未实现 OSS 签名鉴权，需接入 SDK 后再使用。

官方说明：[创建 Bucket 与上传文件](https://www.alibabacloud.com/help/en/oss/user-guide/console-quick-start)、[Bucket ACL](https://www.alibabacloud.com/help/en/oss/user-guide/oss-bucket-acl)。当前控制台默认私有且阻止公共访问，菜单名称以实际控制台为准。

## 2. 准备与上传头像

现有后端启动后，打开 `/preparation.html`：

1. 上传赛前截图。
2. 从左上拖到右下框选游戏内容，排除左右外侧栏；绿色框必须覆盖每个完整头像。
3. 页面下方有 12 张裁图，逐张下载。素材应该是完整卡面，服务端统一去除外围干扰。
4. 根据真实身份命名，例如 `elf-a-normal-v1.png`；不同皮肤使用不同文件名。
5. OSS 文件列表中创建 `avatars/` 目录并上传这些头像。
6. 点击文件详情查看 HTTPS URL；匿名打开应能读取头像。

不要把一个头像的不同精灵误标为同一身份。未确认名称或热门特性的记录先不录入。建议优先覆盖常见阵容，然后逐步补全。

也可以用附带的离线素材裁剪工具（仅用于准备素材，运行服务仍在服务器）：

```sh
java tools/ExtractPreparationAvatars.java /绝对路径/截图.jpg data/avatar-drafts 0.0864 0 0.8273 1
```

最后四个参数是游戏内容区域相对整图的 x、y、宽、高；示例只适合最初提供的截图。输出含 12 张 PNG 和 contact-sheet.png。重新裁剪先用新输出目录，避免覆盖旧素材。

## 3. 配置后端

在服务器部署环境中设置（不要提交真实令牌或数据库密码）：

```text
BATTLE_DB_URL=jdbc:mysql://你的数据库地址:3306/battle_assistant
BATTLE_DB_USER=battle_assistant
BATTLE_DB_PASSWORD=你的数据库密码
BATTLE_AVATAR_BASE_URL=https://你的bucket.oss-cn-hangzhou.aliyuncs.com/
BATTLE_PREPARATION_ADMIN_TOKEN=自行生成的足够长随机令牌
```

OSS 域名中的地域必须与实际 Bucket 一致。基础 URL 不包含 `avatars/`，因为 Key 已包含目录。当前支持无签名查询参数的 HTTPS 地址。

本项目使用 Java 21。执行 `./mvnw package`，将 `target/battle-assistant-0.0.1-SNAPSHOT.jar` 放到服务器，设置上述环境变量后运行：

```sh
java -Djava.awt.headless=true -jar battle-assistant-0.0.1-SNAPSHOT.jar
```

默认服务端口 8080，新表在连接数据库后自动创建。用现有数据库账号，权限参考 README。部署时由 HTTPS 反向代理转发；将旧资料／队伍接口和素材写入接口限制在管理网络内，公开识别接口设置速率和请求大小限制。现有项目未实现完整账号鉴权，不能把所有旧写接口直接公开。识别接口本身最多同时处理 2 个请求，额外请求返回 429。

## 4. 录入一条资料

在 `/preparation.html` 当前头像卡片中填写：

- 头像 Key：`avatars/elf-a-normal-v1.png`，与 OSS 实际路径一致。
- 精灵标识：稳定的英文／数字标识，例如 `elf-a`。同一精灵不同外观用相同标识。
- 名称：真实名称。
- 形态：默认“普通”，不同战斗形态分别命名；只有皮肤不同、战斗形态相同则保持一致。
- 默认热门特性：填写已经确认的常见特性。

在管理令牌输入框填写环境变量中的令牌，点击保存。约一分钟内点击“检查资料状态”：`ready: true` 且 `templates` 为已加载素材数量代表完成。资料不齐或某个文件无法下载时整次预加载失败，先检查所有记录的 Key 与权限。

接口方式（示例资料不代表真实精灵）：

```http
PUT /api/preparation/avatars
Content-Type: application/json
X-Admin-Token: 你的令牌

{
  "avatarKey": "avatars/elf-a-normal-v1.png",
  "speciesKey": "elf-a",
  "name": "实际精灵名称",
  "form": "普通",
  "popularTrait": "实际热门特性"
}
```

同一个 Key 再次 PUT 会覆盖资料。相同精灵标识和形态的各个外观必须填写一致的热门特性。

`GET /api/preparation/avatars` 查看资料，`DELETE /api/preparation/avatars?avatarKey=avatars/xxx.png` 删除错误记录，均需管理令牌。删除后在下一次成功刷新时生效。

## 5. 上传截图识别

`POST /api/preparation/recognize` 使用 multipart/form-data：

| 字段 | 含义 |
| --- | --- |
| image | PNG/JPEG 截图，≤8MB、≤1600 万像素 |
| contentX / contentY | 游戏内容左上角相对整图的比例，默认 0 |
| contentWidth / contentHeight | 游戏内容宽高相对整图的比例，默认 1 |

游戏内容至少 800×400。默认参数只适合没有外围边栏的截图。前端可首次校准区域后在同布局下重复使用。

响应包含 `processingMs`（从进入 Controller 到完成，不包括上传／网络／框架解析），`catalogLoadedAt`（缓存版本时间）、`complete`、`slots`。每个 slot 有 side（self/opponent）、position（1–6）、lead、recognition；identity 内为识别名称、形态及 popularTrait。unknown 时 identity 为 null。没有候选时直接降级，不编造身份。

前端请求设 9 秒超时；这不是服务端硬截止，也不代表已达到十秒 SLA。框架上传超时、反向代理、客户端自动截图触发等需在真实部署链路补充配置与验证。

## 6. 验收

用独立截图测试，不能只把作为模板的同一张截图拿来验证准确率。记录：

- 每个头像及整场 12 只全对率、未知率、误认率。
- 相似形态、皮肤、压缩、缩放和特效情况下的表现。
- 截图开始上传至阵容返回的端到端 P50/P95/P99，包含服务繁忙及冷启动。
- 目标是预热完成后 P95 小于 2 秒、整体小于 10 秒；目前尚无真实部署实测。

自动化测试覆盖确定匹配、未知、相似候选、皮肤合并、输入校验、管理鉴权、空缓存及 12 槽位输出。数据库测试仍按项目原有要求独立启用。

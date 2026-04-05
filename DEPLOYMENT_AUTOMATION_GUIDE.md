# UQBank 自动化上线文档（GHCR 版本）

## 1. 方案结论

已采用 GHCR 作为镜像仓库，并落地自动化发布文件。发布链路如下：

1. 代码 push 到 main。
2. GitHub Actions 自动执行后端和前端构建校验。
3. 自动构建镜像并推送到 GHCR。
4. 服务器 Watchtower 自动检测 prod 标签更新并重启容器。

## 2. 已落地文件

1. CI 工作流：.github/workflows/release-ghcr.yml
2. 后端镜像构建：backend/Dockerfile
3. 前端镜像构建：frontend/Dockerfile
4. 前端 Nginx 配置：frontend/nginx.conf
5. 生产编排模板：deploy/docker-compose.prod.yml
6. 生产环境变量示例：deploy/.env.prod.example

## 3. 镜像标签规则

每次 main 分支发布会推送两类标签：

1. 稳定标签：
   - ghcr.io/your-owner/uqbank-backend:prod
   - ghcr.io/your-owner/uqbank-frontend:prod
2. 不可变标签：
   - ghcr.io/your-owner/uqbank-backend:sha-abcdef0
   - ghcr.io/your-owner/uqbank-frontend:sha-abcdef0

## 4. GitHub 侧配置

### 4.1 仓库权限

1. 确保 Actions 对包有写权限（packages: write）。
2. 建议开启 main 分支保护。

### 4.2 Secrets

当前 workflow 推 GHCR 使用 GITHUB_TOKEN，不强制依赖额外账号密码。

可选 secrets：

1. DEPLOY_WEBHOOK_URL（如未来改成推送式远程触发部署）。

## 5. 服务器部署步骤

### 5.1 准备部署目录

```bash
mkdir -p /opt/uqbank
cd /opt/uqbank
```

将以下文件放到服务器：

1. deploy/docker-compose.prod.yml
2. deploy/.env.prod.example（重命名为 .env 并填写真实值）

### 5.2 服务器登录 GHCR

建议创建只读 PAT（read:packages）用于拉取私有镜像。

```bash
echo "YOUR_GITHUB_PAT" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

### 5.3 初始化并启动

```bash
docker compose -f docker-compose.prod.yml --env-file .env pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

## 6. 自动更新机制

1. workflow 推送新 prod 镜像。
2. Watchtower 每 60 秒轮询。
3. 检测到新镜像后自动 pull 并执行 rolling restart。

## 7. 数据库迁移策略

生产环境建议固定：

1. FLYWAY_ENABLED=true
2. JPA_DDL_AUTO=validate
3. SQL_INIT_MODE=never

说明：数据库结构变更统一走 Flyway 脚本，避免运行期隐式改表。

## 8. 回滚指南

### 8.1 镜像回滚

1. 修改 .env 中 BACKEND_TAG 和 FRONTEND_TAG 为目标 sha 标签。
2. 执行：

```bash
docker compose -f docker-compose.prod.yml --env-file .env pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

### 8.2 数据回滚

1. 优先使用前向修复（追加 Flyway 迁移）。
2. 不可兼容场景再按备份恢复。

## 9. 常见问题

1. GHCR 拉取 401：检查 docker login 账号与 PAT 权限（至少 read:packages）。
2. 镜像名大小写错误：owner 必须为小写，workflow 已自动转换。
3. 自动更新未生效：检查 watchtower 日志和服务标签 com.centurylinklabs.watchtower.enable=true。

## 10. 关联文档

1. 发布清单：DEPLOYMENT_CHECKLIST.md
2. Flyway 迁移目录：backend/src/main/resources/db/migration/postgresql

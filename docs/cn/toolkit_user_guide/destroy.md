# agentarts destroy — 销毁 Agent

`destroy` 命令从华为云删除已部署的 Agent。

> `destroy` 是幂等的：目标 Agent 已不存在时返回成功；认证、权限、限流和服务端错误仍返回非零退出码。

## 用法

```bash
agentarts destroy [选项]
```

## 参数

| 参数 | 缩写 | 说明 | 默认值 |
|------|------|------|--------|
| `--agent` | `-a` | Agent 名称 | 配置中的默认 Agent |
| `--region` | `-r` | 华为云区域 | 配置文件值 |
| `--yes` | `-y` | 跳过确认提示 | `false` |
| `--skip-ssl-verification` | `-k` | 跳过 SSL 证书验证 | `false` |

## 示例

```bash
# 交互式销毁
agentarts destroy

# 指定 Agent
agentarts destroy --agent my-agent

# 指定区域
agentarts destroy --agent my-agent --region cn-southwest-2

# 跳过确认
agentarts destroy --agent my-agent --yes

# 完整参数
agentarts destroy -a my-agent -r cn-southwest-2 -y
```

## 删除的资源

CLI 按名称解析 Agent 并调用 Runtime 控制面的 Agent 删除接口。服务端是否级联清理端点或运行时内部资源，以云服务契约为准；CLI 不会伪造级联删除成功。

## 未删除的资源

以下资源不会被自动删除，需要手动清理：

- SWR 镜像
- IAM 代理
- 配置文件（`.agentarts_config.yaml`）
- CLI 不会显式删除的其他外部资源

## 删除前检查

```bash
# 确认 Agent 配置
agentarts config get base.name --agent my-agent

# 删除
agentarts destroy -a my-agent -y

# 清理配置
agentarts config remove my-agent
```

## 批量删除

```bash
# 批量删除脚本
for agent in agent-1 agent-2 agent-3; do
    agentarts destroy -a $agent -y
done
```

```powershell
# 批量删除
foreach ($agent in "agent-1","agent-2","agent-3") {
    agentarts destroy -a $agent -y
}
```

## CI/CD 集成

```yaml
# .gitlab-ci.yml
cleanup:
  script:
    - agentarts destroy -a $AGENT_NAME -y
  when: manual
```

## 注意事项

1. **删除操作不可逆**，请确认后再执行
2. 建议使用 `--yes` 仅用于自动化场景
3. 删除后如需重新部署，执行 `agentarts deploy`
4. 生产环境保持 SSL 验证开启，并在删除后核对 SWR、IAM 和计费资源清单

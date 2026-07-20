#!/usr/bin/env bash
# 一键运行 AgentArts examples demo
# 用法:
#   ./run-demo.sh              # 默认跑导航 demo（NavigationLongTermMemoryDemo）
#   ./run-demo.sh nav          # 同上
#   ./run-demo.sh memory       # MemoryUsageExample
#   ./run-demo.sh agentscope   # AgentScopeIntegrationExample
#   ./run-demo.sh basic        # BasicRuntimeExample
#   ./run-demo.sh com.huaweicloud.agentarts.examples.Xxx  # 传完整类名
#
# 环境变量从同目录 .env 自动加载（cp .env.example .env 后填写）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MODULE="agentarts-sdk-examples"

# ---- 加载 .env ----
ENV_FILE="$SCRIPT_DIR/.env"
if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$ENV_FILE"
    set +a
    echo "[run-demo] 已加载 ${ENV_FILE}"
else
    echo "[run-demo] ⚠ 未找到 ${ENV_FILE}，使用现有环境变量。可: cp .env.example .env"
fi

# ---- 解析 demo 类名 ----
ARG="${1:-nav}"
case "$ARG" in
    nav|navigation) MAIN="com.huaweicloud.agentarts.examples.memory.NavigationLongTermMemoryDemo" ;;
    memory)         MAIN="com.huaweicloud.agentarts.examples.memory.MemoryUsageExample" ;;
    agentscope)     MAIN="com.huaweicloud.agentarts.examples.AgentScopeIntegrationExample" ;;
    basic)          MAIN="com.huaweicloud.agentarts.examples.BasicRuntimeExample" ;;
    com.huaweicloud.*) MAIN="$ARG" ;;
    *) echo "[run-demo] 未知 demo: $ARG"; exit 1 ;;
esac
echo "[run-demo] 运行 $MAIN"

cd "$REPO_ROOT"

# ---- 首次运行：确保 SDK 模块已 install 到本地 m2 ----
if ! find "$HOME/.m2/repository/com/huaweicloud/agentarts/agentarts-sdk-integration-agentscope" -name "*.jar" 2>/dev/null | grep -q .; then
    echo "[run-demo] 首次运行，先 install SDK 到本地 m2（一次性，较慢）..."
    mvn -q install -DskipTests
fi

# ---- 编译 + 运行 ----
mvn -q compile exec:java -pl "$MODULE" -Dexec.mainClass="$MAIN" -Dexec.classpathScope=runtime

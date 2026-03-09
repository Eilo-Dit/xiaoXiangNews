#!/usr/bin/env bash
# 自动执行 adb pair 和 adb connect 指令
# 使用方式:
#   1. 直接运行脚本后, 按提示输入配对端口 (PORT_PAIR)
#   2. 或者在命令行一次性传递配对端口: PORT_PAIR
# 示例: ./adb_link.sh 5555

set -euo pipefail
IP="192.168.3.19"
PORT_CONNECT="46757"   # 固定连接端口
# 如果以参数形式传入, 直接使用参数, 否则交互式读取
if [[ $# -ge 1 ]]; then
  PORT_PAIR="$1"
else
  read -rp "请输入用于 adb pair 的端口号: " PORT_PAIR
fi

# 校验基本格式 (简单校验, 不深入解析)
if [[ -z "$IP" || -z "$PORT_PAIR" || -z "$PORT_CONNECT" ]]; then
  echo "IP 或端口不能为空, 请重新执行脚本。" >&2
  exit 1
fi

echo "\n开始执行: adb pair ${IP}:${PORT_PAIR}"
adb pair "${IP}:${PORT_PAIR}"

echo "\n开始执行: adb connect ${IP}:${PORT_CONNECT}"
adb connect "${IP}:${PORT_CONNECT}"

echo "\n执行完成。"

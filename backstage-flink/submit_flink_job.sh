#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_JAR_PATH="$SCRIPT_DIR/target/backstage-flink-3.9.0.jar"
DEFAULT_FLINK_BIN="${FLINK_BIN:-flink}"
DEFAULT_REST_ADDRESS="${FLINK_REST_ADDRESS:-}"
DEFAULT_ACTION="${FLINK_ACTION:-run}"

show_usage() {
  cat <<'EOF'
用法：
  ./submit_flink_job.sh -c <mainClass> [options] -- [job args...]

通用参数：
  -a, --action <run|run-application|info>
                                  可选，默认 run
  -c, --class <mainClass>         必填，作业入口类
  -j, --jar <jarPath>             可选，默认使用 target/backstage-flink-3.9.0.jar
  -p, --parallelism <num>         可选，设置 Flink 提交并行度，默认 1
  -d, --detached                  可选，使用 detached 模式提交
  -n, --job-name <jobName>        可选，设置作业显示名称
  -r, --rest <host:port>          可选，指定 REST 地址，例如 127.0.0.1:30081
  -t, --target <target>           可选，设置 execution.target
      --from-savepoint <path>     可选，从 savepoint/checkpoint 恢复
      --allow-non-restored-state  可选，恢复时允许跳过未映射状态
      --restore-mode <mode>       可选，CLAIM / NO_CLAIM / LEGACY
      --classpath <path>          可选，附加 classpath，可重复传入
  -D, --dynamic <key=value>       可选，透传 Flink 动态配置，可重复传入

JVM 参数：
      --all-jvm-opts <opts>       设置 env.java.opts.all
      --jm-jvm-opts <opts>        设置 env.java.opts.jobmanager
      --tm-jvm-opts <opts>        设置 env.java.opts.taskmanager

说明：
  1. 作业参数请放到 -- 后面，例如 -- --heapMb 64 --parallelism 1
  2. JVM 参数本质上会转成 Flink 的 -D 动态配置
  3. 在 session cluster 中，JM/TM JVM 参数是否生效取决于集群部署模式
  4. 如需完全自定义 Flink 命令，可通过 FLINK_BIN 传入，例如：
     FLINK_BIN="docker exec flink-jobmanager-1 /opt/flink/bin/flink"

示例：
  ./submit_flink_job.sh \
    -c com.bachstage.memory.stress.HeapMemoryHoldJob \
    -p 1 \
    -n heap-64m \
    --tm-jvm-opts "-Xms256m -Xmx256m" \
    -- --heapMb 64 --parallelism 1

  ./submit_flink_job.sh \
    -c com.bachstage.memory.stress.ManagedMemoryProfileJob \
    -r 127.0.0.1:30081 \
    -D taskmanager.numberOfTaskSlots=4 \
    -- --managedMb 64 --taskHeapMb 64

  ./submit_flink_job.sh \
    --action run \
    -c com.bachstage.memory.stress.HeapMemoryHoldJob \
    --from-savepoint file:///tmp/savepoint-xxx \
    --allow-non-restored-state \
    --restore-mode CLAIM \
    --classpath /opt/flink/usrlib/ext.jar \
    -- --heapMb 16
EOF
}

require_value() {
  local flag="$1"
  local value="${2:-}"
  if [[ -z "$value" ]]; then
    echo "参数 $flag 缺少值" >&2
    show_usage
    exit 1
  fi
}

MAIN_CLASS=""
JAR_PATH="$DEFAULT_JAR_PATH"
ACTION="$DEFAULT_ACTION"
PARALLELISM="1"
DETACHED="false"
JOB_NAME=""
REST_ADDRESS="$DEFAULT_REST_ADDRESS"
TARGET=""
FROM_SAVEPOINT=""
ALLOW_NON_RESTORED_STATE="false"
RESTORE_MODE=""

declare -a FLINK_ARGS
declare -a JOB_ARGS
declare -a CLASSPATH_ARGS

while [[ $# -gt 0 ]]; do
  case "$1" in
    -a|--action)
      require_value "$1" "${2:-}"
      ACTION="$2"
      shift 2
      ;;
    -c|--class)
      require_value "$1" "${2:-}"
      MAIN_CLASS="$2"
      shift 2
      ;;
    -j|--jar)
      require_value "$1" "${2:-}"
      JAR_PATH="$2"
      shift 2
      ;;
    -p|--parallelism)
      require_value "$1" "${2:-}"
      PARALLELISM="$2"
      shift 2
      ;;
    -d|--detached)
      DETACHED="true"
      shift
      ;;
    -n|--job-name)
      require_value "$1" "${2:-}"
      JOB_NAME="$2"
      shift 2
      ;;
    -r|--rest)
      require_value "$1" "${2:-}"
      REST_ADDRESS="$2"
      shift 2
      ;;
    -t|--target)
      require_value "$1" "${2:-}"
      TARGET="$2"
      shift 2
      ;;
    --from-savepoint)
      require_value "$1" "${2:-}"
      FROM_SAVEPOINT="$2"
      shift 2
      ;;
    --allow-non-restored-state)
      ALLOW_NON_RESTORED_STATE="true"
      shift
      ;;
    --restore-mode)
      require_value "$1" "${2:-}"
      RESTORE_MODE="$2"
      shift 2
      ;;
    --classpath)
      require_value "$1" "${2:-}"
      CLASSPATH_ARGS+=("-C" "$2")
      shift 2
      ;;
    -D|--dynamic)
      require_value "$1" "${2:-}"
      FLINK_ARGS+=("-D$2")
      shift 2
      ;;
    --all-jvm-opts)
      require_value "$1" "${2:-}"
      FLINK_ARGS+=("-Denv.java.opts.all=$2")
      shift 2
      ;;
    --jm-jvm-opts)
      require_value "$1" "${2:-}"
      FLINK_ARGS+=("-Denv.java.opts.jobmanager=$2")
      shift 2
      ;;
    --tm-jvm-opts)
      require_value "$1" "${2:-}"
      FLINK_ARGS+=("-Denv.java.opts.taskmanager=$2")
      shift 2
      ;;
    --)
      shift
      JOB_ARGS=("$@")
      break
      ;;
    -h|--help)
      show_usage
      exit 0
      ;;
    *)
      echo "无法识别的参数: $1" >&2
      show_usage
      exit 1
      ;;
  esac
done

case "$ACTION" in
  run|run-application|info)
    ;;
  *)
    echo "暂不支持的 action: $ACTION" >&2
    show_usage
    exit 1
    ;;
esac

if [[ "$ACTION" != "info" && -z "$MAIN_CLASS" ]]; then
  echo "action=$ACTION 时，必须通过 -c 或 --class 指定 mainClass" >&2
  show_usage
  exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "未找到 jar: $JAR_PATH" >&2
  exit 1
fi

if [[ -n "$PARALLELISM" ]]; then
  FLINK_ARGS+=("-p" "$PARALLELISM")
fi

if [[ "$DETACHED" == "true" ]]; then
  FLINK_ARGS+=("-d")
fi

if [[ -n "$JOB_NAME" ]]; then
  FLINK_ARGS+=("-Dpipeline.name=$JOB_NAME")
fi

if [[ -n "$REST_ADDRESS" ]]; then
  FLINK_ARGS+=("-m" "$REST_ADDRESS")
fi

if [[ -n "$TARGET" ]]; then
  FLINK_ARGS+=("-Dexecution.target=$TARGET")
fi

if [[ -n "$FROM_SAVEPOINT" ]]; then
  FLINK_ARGS+=("-s" "$FROM_SAVEPOINT")
fi

if [[ "$ALLOW_NON_RESTORED_STATE" == "true" ]]; then
  FLINK_ARGS+=("-n")
fi

if [[ -n "$RESTORE_MODE" ]]; then
  FLINK_ARGS+=("-restoreMode" "$RESTORE_MODE")
fi

read -r -a FLINK_BIN_CMD <<< "$DEFAULT_FLINK_BIN"
COMMAND=("${FLINK_BIN_CMD[@]}" "$ACTION")

if [[ -n "$MAIN_CLASS" ]]; then
  COMMAND+=("-c" "$MAIN_CLASS")
fi

COMMAND+=("${FLINK_ARGS[@]}")
COMMAND+=("${CLASSPATH_ARGS[@]}")
COMMAND+=("$JAR_PATH")
COMMAND+=("${JOB_ARGS[@]}")

echo "执行命令："
printf ' %q' "${COMMAND[@]}"
echo

"${COMMAND[@]}"

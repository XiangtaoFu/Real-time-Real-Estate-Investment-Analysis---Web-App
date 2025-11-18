#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/.."
cd "${ROOT_DIR}/realtyinus"

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "[info] JAVA_HOME is not set. Using system java in PATH (prefer JDK 21 or 17)."
fi

# Prefer wrapper for consistent Maven
if [[ -x "./mvnw" ]]; then
  ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
else
  mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
fi

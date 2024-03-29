#!/usr/bin/env bash
# set up logging utilities
source $(dirname $0)/utils.sh

# shared variables across all scripts
ROOT_DIR="$(cd "$(dirname "$0")/.."; pwd)"
SCRIPTS_DIR=${SCRIPTS_DIR:-${ROOT_DIR}/scripts}
TMP_DIR=${TMP_DIR:-${ROOT_DIR}/build/tmp}

DEBUG_OPTS=""

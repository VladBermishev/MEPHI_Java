#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
EXEDIR=$SCRIPT_DIR

source $SCRIPT_DIR/common/elevate.sh

[[ $EUID -ne 0 ]] && { require_root; echo "A"; exit 0; }
echo "B"



#!/bin/bash
# Ensure that there are no uncommitted submodule changes.

clean=$(git status | grep "modified: *submodules/")
if [ ! -z "$clean" ]; then
    echo Error: There are uncommitted submodule changes
    echo $clean
    exit 1
fi

echo "Submodules clean"
exit 0
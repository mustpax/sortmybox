#!/bin/bash
# @author mustpax
# First ensure that the branch is clean.
# Then check that the current branch matches the first argument to script, if
# not exit with error.

clean=$(git status | grep "nothing to commit (working directory clean)")
if [ -z "$clean" ]; then
    echo Error: There are uncommitted changes
    exit 1
else
    echo Branch is clean
fi

branch=$(git symbolic-ref -q HEAD | awk -F/ '{print $NF}')
if [ "$1" = "$branch" ]; then
    exit 0
fi

echo Error: Must be on branch $1 Current branch $branch
exit 1

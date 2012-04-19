#!/bin/bash
# @author mustpax
# Display error message and exit with status code 1 if current git branch does
# not match first argument. Otherwise silently exit with status code 0.

branch=$(git symbolic-ref -q HEAD | awk -F/ '{print $NF}')
if [ "$1" = "$branch" ]; then
    exit 0
fi

echo Error: Must be on branch $1 Current branch $branch
exit 1

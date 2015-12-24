#!/bin/sh
checkPid=`ps aux | grep check.sh | grep -v grep | tail -1 | awk '{print $2}'`
if [[ "$checkPid" != "" ]]; then
  tail -f --pid=$checkPid $1
else
  >&2 echo "There is no check.sh running"
fi

#! /bin/sh

#
# Usage: ./bin/check.sh path/to/log process-feature max-seconds
#

log_file=$1
system=$2
max=$3

if [ "$max" == "" ]; then
  max=300
fi

echo "Checking process: $system by $log_file, until $max seconds"

for((i=0;i<$max;i++)) do
  sleep 1
  # check failed
  process=`ps aux | grep $2 | grep -v grep | grep -v check.sh`
  if [ "$process" == "" ]; then
    echo "$system is stopped"
    exit 255
  else
    # check started
    if grep -i "$system is started" $log_file
    then
      echo "$system is running and ready"
      exit 0
    else
      #echo "$system is running but not ready"
      printf "."
    fi
  fi
done

echo "$system is not started until $max seconds"
exit 1

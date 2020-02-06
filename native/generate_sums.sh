#!/bin/bash
me=`basename "$0"`
oldIFS="$IFS"
IFS=$'\n'

for line in `find . -type f -exec sha1sum '{}' '+'`; do
    IFS=$' \t'
    ray=($line)
    sum=${ray[0]}
    name=${ray[1]}
    IFS=$'\n'

    name="${name:2}"
    if [ "$name" != "$me" ]; then
        echo "\"$name\": \"$sum\","
    fi
done

IFS="$oldIFS"


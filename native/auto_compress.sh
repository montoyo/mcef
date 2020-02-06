#!/bin/bash
echo "Compressing large files..."
find . -type f -size '+1M' -not -iname '*.zip' -not -iname '*.gz' -print -exec gzip '{}' ';' > compressed.txt
echo "The list of files that have been compressed can be found in compressed.txt"
echo "Don't forget to append an @ in config2.json"


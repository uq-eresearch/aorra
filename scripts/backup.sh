#!/bin/bash
 
DEST=$1
MAX=$2
PREFIX=$3
 
# Drop the 3 arguments above, the remaining arguments are the files to be backed up, accessible via "$@".
shift 3
 
# You can remove the "-a $# -gt 0" if there are no files to be backed up; however, as your backup should
# include at least /etc this is not considered likely.
 
if ! [ -d "$DEST" -a -w "$DEST" -a -n "$MAX" -a -n "$PREFIX" -a $# -gt 0 ]; then
  echo "Usage: $(dirname $0) DEST MAX USER PASSWORD PREFIX FILES"
  echo "DEST is the directory to write the backup to (must exist and be writable)"
  echo "MAX is the maximum number of backups with this prefix to keep (old ones are deleted)"
  echo "PREFIX is a prefix to disambiguate this backup set"
  echo "FILES is a space-separated list of file and/or directory paths to be backed-up"
  exit 1
fi
 
TIMESTAMP=$(date '+%Y%m%d%H%M%S')
FS_BACKUP="$DEST/$PREFIX-backup-$TIMESTAMP.tar.bz2"
 
tar -c -j -f $FS_BACKUP -P -C / "$@"
 
VICTIMS="$(ls -1 $DEST/$PREFIX-backup-*.tar.bz2 | head -n-$MAX)"
for VICTIM in $VICTIMS; do
  rm $VICTIM
done


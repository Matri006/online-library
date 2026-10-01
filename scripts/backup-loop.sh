#!/bin/sh
set -eu
umask 077
while true; do
  name="/backups/library-$(date -u +%Y%m%dT%H%M%SZ).dump"
  if pg_dump -Fc --file="$name.tmp"; then
    mv "$name.tmp" "$name"
    find /backups -name 'library-*.dump' -mtime +30 -delete
    echo "Backup completed: $name"
  else
    echo "Backup failed; retry in 5 minutes" >&2
    sleep 300
    continue
  fi
  sleep 86400
done

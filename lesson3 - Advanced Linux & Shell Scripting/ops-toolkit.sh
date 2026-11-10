#!/usr/bin/env bash
#
# ops-toolkit.sh - daily operations helper for a small web server
#
# Usage: ./ops-toolkit.sh {logs|backup|disk|services|all} [options]
#
# Real-life jobs it covers:
#   logs      Analyse the nginx access log (who is hitting us? any errors?)
#   backup    Compress config + web files, verify, delete backups > 7 days old
#   disk      Warn when a filesystem is nearly full and show what is eating it
#   services  Make sure key services are running, restart them if not
#
# Note: pipefail is deliberately NOT set. `... | head` closes the pipe early and
# pipefail would turn that harmless SIGPIPE into a script failure.

set -u

# ---------- Configuration (override with env vars) ----------
LOG_FILE="${LOG_FILE:-/var/log/nginx/access.log}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"
BACKUP_SRC=(/etc/nginx /var/www)
RETENTION_DAYS="${RETENTION_DAYS:-7}"
DISK_THRESHOLD="${DISK_THRESHOLD:-80}"      # percent
REQ_LIMIT="${REQ_LIMIT:-100}"               # requests per IP before "suspicious"
SERVICES=(nginx ssh cron)
OPS_LOG="${OPS_LOG:-$HOME/ops-toolkit.log}"

# ---------- Helpers ----------
log()  { echo "$(date '+%F %T') [$1] $2" | tee -a "$OPS_LOG"; }
info() { log INFO "$1"; }
warn() { log WARN "$1"; }
die()  { log ERROR "$1"; exit 1; }

# ---------- 1. Log analysis ----------
analyze_logs() {
    local top_n="${1:-5}"
    [[ -r "$LOG_FILE" ]] || die "Cannot read $LOG_FILE (try sudo?)"

    echo "===== Access log report: $LOG_FILE ====="
    echo "Total requests : $(wc -l < "$LOG_FILE")"
    echo "Server errors  : $(grep -cE '" 5[0-9]{2} ' "$LOG_FILE")"
    echo

    echo "--- HTTP status codes ---"
    awk '{print $9}' "$LOG_FILE" | sort | uniq -c | sort -rn
    echo

    echo "--- Top $top_n client IPs ---"
    awk '{print $1}' "$LOG_FILE" | sort | uniq -c | sort -rn | head -n "$top_n"
    echo

    echo "--- Top $top_n requested URLs ---"
    awk '{print $7}' "$LOG_FILE" | sort | uniq -c | sort -rn | head -n "$top_n"
    echo

    echo "--- Top $top_n URLs returning 404 ---"
    awk '$9 == 404 {print $7}' "$LOG_FILE" | sort | uniq -c | sort -rn | head -n "$top_n"
    echo

    echo "--- Requests per hour ---"
    # $4 looks like [10/Nov/2026:14:03:22 -> keep "date:hour"
    awk '{split($4, t, ":"); print substr(t[1], 2) " " t[2] ":00"}' "$LOG_FILE" \
        | sort | uniq -c
    echo

    echo "--- Suspicious IPs (> $REQ_LIMIT requests) ---"
    awk -v limit="$REQ_LIMIT" '{c[$1]++} END {for (ip in c) if (c[ip] > limit) print c[ip], ip}' \
        "$LOG_FILE" | sort -rn
}

# ---------- 2. Backup + rotation ----------
do_backup() {
    local stamp file src=()
    stamp=$(date '+%F_%H%M%S')
    file="$BACKUP_DIR/backup-$(hostname)-$stamp.tar.gz"
    mkdir -p "$BACKUP_DIR" || die "Cannot create $BACKUP_DIR"

    for dir in "${BACKUP_SRC[@]}"; do
        if [[ -d "$dir" ]]; then
            src+=("$dir")
        else
            warn "Skipping $dir (does not exist)"
        fi
    done
    [[ ${#src[@]} -gt 0 ]] || die "Nothing to back up"

    info "Backing up: ${src[*]}"
    tar -czf "$file" "${src[@]}" 2>/dev/null || warn "tar reported warnings (unreadable files?)"

    # A backup you never verified is not a backup
    if tar -tzf "$file" > /dev/null 2>&1; then
        info "Backup OK: $file ($(du -h "$file" | cut -f1))"
    else
        rm -f "$file"
        die "Backup is corrupt and was removed"
    fi

    info "Deleting backups older than $RETENTION_DAYS days"
    find "$BACKUP_DIR" -name 'backup-*.tar.gz' -type f -mtime +"$RETENTION_DAYS" -print -delete \
        | while read -r old; do info "Removed old backup: $old"; done
}

# ---------- 3. Disk space ----------
check_disk() {
    local found=0
    # -P = POSIX output, one line per filesystem (no wrapping)
    while read -r fs size used _ pct mount; do
        local usage=${pct%\%}
        if (( usage >= DISK_THRESHOLD )); then
            warn "$mount is ${pct} full ($used of $size used on $fs)"
            echo "  Biggest directories under $mount:"
            du -xh --max-depth=1 "$mount" 2>/dev/null | sort -rh | head -6 | sed 's/^/    /'
            found=1
        fi
    done < <(df -hP | awk 'NR > 1 && $1 ~ /^\/dev\//')

    echo "  Log files bigger than 100MB:"
    find /var/log -type f -size +100M -exec ls -lh {} \; 2>/dev/null \
        | awk '{print "    " $5, $9}'

    [[ $found -eq 0 ]] && info "All filesystems below ${DISK_THRESHOLD}%"
}

# ---------- 4. Service watchdog ----------
check_services() {
    for svc in "${SERVICES[@]}"; do
        if systemctl is-active --quiet "$svc"; then
            info "$svc is running"
        else
            warn "$svc is DOWN - restarting"
            if sudo systemctl restart "$svc" && systemctl is-active --quiet "$svc"; then
                info "$svc restarted successfully"
            else
                log ERROR "$svc failed to restart - check: journalctl -u $svc -n 50"
            fi
        fi
    done
}

usage() {
    cat <<EOF
Usage: $0 <command> [options]

Commands:
  logs [N]    Access log report, top N entries (default 5)
  backup      Create backup and rotate old ones
  disk        Check disk usage against ${DISK_THRESHOLD}% threshold
  services    Check/restart: ${SERVICES[*]}
  all         Run disk + services + backup

Env overrides: LOG_FILE BACKUP_DIR RETENTION_DAYS DISK_THRESHOLD REQ_LIMIT OPS_LOG
EOF
}

# ---------- Main ----------
case "${1:-}" in
    logs)     analyze_logs "${2:-5}" ;;
    backup)   do_backup ;;
    disk)     check_disk ;;
    services) check_services ;;
    all)      check_disk; check_services; do_backup ;;
    *)        usage; exit 1 ;;
esac

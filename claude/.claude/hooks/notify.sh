#!/usr/bin/env bash
# Desktop notification when Claude finishes (Stop) or needs input (Notification).
input=$(cat)
# Read a string field from the JSON input without jq
json_field() {
  echo "$input" | sed -n "s/.*\"$1\" *: *\"\([^\"]*\)\".*/\1/p" | head -n1
}
event=$(json_field hook_event_name)

if [ "$event" = "Notification" ]; then
  msg=$(json_field message)
  msg=${msg:-Claude needs your input}
else
  msg="Claude Code has finished"
fi
title="Claude Code"

case "$(uname -s)" in
  Linux*)
    icon="$(dirname "$0")/claude-icon.png"
    if command -v gdbus >/dev/null; then
      # Send directly to the desktop notification service over D-Bus
      gdbus call --session --dest org.freedesktop.Notifications \
        --object-path /org/freedesktop/Notifications \
        --method org.freedesktop.Notifications.Notify \
        "$title" 0 "$icon" "$title" "$msg" "[]" "{}" 10000 >/dev/null 2>&1
    else
      printf '\a'  # last resort: terminal bell
    fi
    command -v paplay >/dev/null && paplay /usr/share/sounds/freedesktop/stereo/complete.oga 2>/dev/null &
    ;;
  Darwin*)
    osascript -e "display notification \"$msg\" with title \"$title\""
    afplay /System/Library/Sounds/Glass.aiff &
    ;;
  MINGW*|MSYS*|CYGWIN*)
    powershell.exe -NoProfile -Command "Add-Type -AssemblyName System.Windows.Forms; \$n=New-Object System.Windows.Forms.NotifyIcon; \$n.Icon=[System.Drawing.SystemIcons]::Information; \$n.Visible=\$true; \$n.ShowBalloonTip(5000,'$title','$msg','Info'); [System.Media.SystemSounds]::Asterisk.Play(); Start-Sleep -Seconds 6; \$n.Dispose()" ;;
esac
exit 0

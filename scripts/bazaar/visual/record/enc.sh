#!/bin/bash
# enc.sh <recorded.mp4> <name> <docsBox> [docsCrf] [docsFps]
#   -> $ENC/full/<name>.mp4  (the recording re-encoded at its own size and rate)
#      $ENC/docs/<name>.mp4  (fitted inside docsBox x docsBox, for the website)
#      $ENC/docs/<name>-poster.jpg and $ENC/sheet-<name>.jpg (8 frames, for review)
# ENC defaults to scripts/bazaar/target/record/enc. CROP_TOP=<px> removes that many rows
# from the top first: a desktop recording carries the simulator's menu bar (21 px).
set -euo pipefail
IN=$1; N=$2; BOX=$3; CRF=${4:-28}; DFPS=${5:-60}
ENC=${ENC:-$(cd "$(dirname "$0")/../.." && pwd)/target/record/enc}
mkdir -p "$ENC/full" "$ENC/docs"
CROP="crop=iw:ih-${CROP_TOP:-0}:0:${CROP_TOP:-0},crop=trunc(iw/2)*2:trunc(ih/2)*2:0:0"
FIT="$CROP,scale=w=$BOX:h=$BOX:force_original_aspect_ratio=decrease:force_divisible_by=2:flags=lanczos"
ffmpeg -v error -y -i "$IN" -vf "$CROP,format=yuv420p" -c:v libx264 -preset slow -crf 18 -an -movflags +faststart "$ENC/full/$N.mp4"
ffmpeg -v error -y -i "$IN" -vf "fps=$DFPS,$FIT,format=yuv420p" -c:v libx264 -preset veryslow -crf "$CRF" -an -movflags +faststart "$ENC/docs/$N.mp4"
DUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$IN")
ffmpeg -v error -y -ss "$(echo "$DUR*0.3" | bc)" -i "$IN" -frames:v 1 -vf "$FIT" -q:v 3 "$ENC/docs/$N-poster.jpg"
ffmpeg -v error -y -i "$IN" -vf "$CROP,fps=8/$DUR,scale=480:-2,tile=4x2" -frames:v 1 "$ENC/sheet-$N.jpg"
echo "$N full=$(wc -c <"$ENC/full/$N.mp4") docs=$(wc -c <"$ENC/docs/$N.mp4") dur=$DUR"

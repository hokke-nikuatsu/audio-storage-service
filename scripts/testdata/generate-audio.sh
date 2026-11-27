#!/usr/bin/env bash
# 変換のテストで受け付けないファイルを、受け付ける m4a から ffmpeg で作り、testdata/audio/ に置く。
# 受け付ける m4a は人が用意する（MP4 のコンテナに AAC の音声を入れたもの）。
set -euo pipefail

AUDIO_DIR="$(cd "$(dirname "$0")/../../testdata/audio" && pwd)"
SOURCE="${AUDIO_DIR}/eagle-has-landed.m4a"

# 受け付ける m4a が、MP4 のコンテナに AAC の音声を入れたものであることを確かめる
format="$(ffprobe -v error -show_entries format=format_name -of default=nw=1:nk=1 "$SOURCE")"
codec="$(ffprobe -v error -select_streams a:0 -show_entries stream=codec_name -of default=nw=1:nk=1 "$SOURCE")"
if [[ "$format" != *mp4* || "$codec" != "aac" ]]; then
  echo "エラー: ${SOURCE} が MP4 と AAC ではない（format: ${format}、codec: ${codec}）。根拠: .kiro/specs/audio-storage/tasks.md のタスク 7.1" >&2
  exit 1
fi

# 作り直しても同じ中身になるよう、ffmpeg のバージョンなどのメタデータを書かない
convert() {
  ffmpeg -v error -y -i "$SOURCE" -map_metadata -1 -fflags +bitexact -flags:a +bitexact "$@"
}

# 音声ではないテキストを m4a と名付けたもの
printf 'これは音声ではない\n' >"${AUDIO_DIR}/not-audio.m4a"
# m4a ではない音声の形式
convert -c:a pcm_s16le "${AUDIO_DIR}/eagle-has-landed.wav"
convert -c:a libmp3lame "${AUDIO_DIR}/eagle-has-landed.mp3"
# MP4 のコンテナでも、AAC ではないもの（ボイスメモの「ロスレス」の録音と同じ ALAC）
convert -c:a alac "${AUDIO_DIR}/eagle-has-landed-alac.m4a"
# アップロードが途中で切れたもの。目次（moov）がファイルの末尾にあるので、前半だけを残すと目次がなくなる
size="$(wc -c <"$SOURCE")"
truncated="${AUDIO_DIR}/eagle-has-landed-truncated.m4a"
head -c "$((size / 2))" "$SOURCE" >"$truncated"
# 目次が先頭にある m4a だと、前半だけでも読めてしまい、途中で切れたファイルにならない
if ffprobe -v error -f mov "$truncated" 2>/dev/null; then
  echo "エラー: ${truncated} を ffprobe が読めてしまう。${SOURCE} の目次（moov）がファイルの末尾にない" >&2
  exit 1
fi

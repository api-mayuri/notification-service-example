#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper (only-script). Скачивает дистрибутив Maven, указанный в
# .mvn/wrapper/maven-wrapper.properties, в ~/.m2/wrapper/dists и запускает его.
# ----------------------------------------------------------------------------
set -euf

die() { printf 'mvnw: %s\n' "$*" >&2; exit 1; }

# Каталог проекта: ищем .mvn вверх от текущего каталога
base_dir=$(pwd)
while [ ! -d "$base_dir/.mvn" ] && [ "$base_dir" != "/" ]; do
  base_dir=$(dirname "$base_dir")
done
[ -d "$base_dir/.mvn" ] || base_dir=$(cd "$(dirname "$0")" && pwd)

properties="$base_dir/.mvn/wrapper/maven-wrapper.properties"
[ -f "$properties" ] || die "не найден $properties"

distribution_url=$(sed -n 's/^distributionUrl[[:space:]]*=[[:space:]]*//p' "$properties" | tr -d '\r')
[ -n "$distribution_url" ] || die "distributionUrl не задан в $properties"
[ -z "${MVNW_REPOURL:-}" ] || distribution_url="$MVNW_REPOURL/org/apache/maven/${distribution_url#*/org/apache/maven/}"

distribution_name=$(basename "$distribution_url")
distribution_dir_name=${distribution_name%-bin.zip}
hash=$(printf '%s' "$distribution_url" | cksum | cut -d ' ' -f 1)
maven_user_home=${MAVEN_USER_HOME:-"$HOME/.m2"}
install_root="$maven_user_home/wrapper/dists/$distribution_dir_name/$hash"
maven_home="$install_root/$distribution_dir_name"

if [ ! -x "$maven_home/bin/mvn" ]; then
  mkdir -p "$install_root"
  tmp_dir=$(mktemp -d "$install_root/tmp.XXXXXX")
  trap 'rm -rf "$tmp_dir"' EXIT
  printf 'mvnw: загрузка %s\n' "$distribution_url" >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$tmp_dir/$distribution_name" "$distribution_url" || die "не удалось скачать Maven"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$tmp_dir/$distribution_name" "$distribution_url" || die "не удалось скачать Maven"
  else
    die "нужен curl или wget"
  fi
  command -v unzip >/dev/null 2>&1 || die "нужен unzip"
  unzip -q "$tmp_dir/$distribution_name" -d "$tmp_dir" || die "не удалось распаковать Maven"
  extracted=$(find "$tmp_dir" -mindepth 1 -maxdepth 1 -type d -name 'apache-maven-*' | head -n 1)
  [ -n "$extracted" ] || die "в архиве нет каталога apache-maven-*"
  maven_home="$install_root/$(basename "$extracted")"
  rm -rf "$maven_home"
  mv "$extracted" "$maven_home"
fi

exec "$maven_home/bin/mvn" "$@"

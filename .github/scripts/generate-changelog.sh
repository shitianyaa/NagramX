#!/usr/bin/env bash

set -euo pipefail

output_path=${1:-release-notes/notes.md}
channel=${RELEASE_CHANNEL:-stable}
current_sha=${CURRENT_SHA:-${GITHUB_SHA:-HEAD}}

case "$channel" in
  staging)
    tag_pattern='^v?[0-9]+\.[0-9]+\.[0-9]+([.-][0-9]+)?-staging([.-][0-9]+)?$'
    ;;
  stable)
    tag_pattern='^v?[0-9]+\.[0-9]+\.[0-9]+([.-][0-9]+)?$'
    ;;
  *)
    echo "Unsupported release channel: $channel" >&2
    exit 1
    ;;
esac

previous_tag=""
while IFS= read -r tag; do
  [[ -z "$tag" ]] && continue
  tag_sha=$(git rev-list -n 1 "$tag")
  if [[ "$tag_sha" != "$current_sha" ]] && git merge-base --is-ancestor "$tag_sha" "$current_sha"; then
    previous_tag=$tag
    break
  fi
done < <(git tag --sort=-version:refname | grep -E "$tag_pattern" || true)

previous_sha=""
if [[ -n "$previous_tag" ]]; then
  previous_sha=$(git rev-list -n 1 "$previous_tag")
else
  while IFS= read -r version_sha; do
    [[ -z "$version_sha" || "$version_sha" == "$current_sha" ]] && continue
    previous_sha=$version_sha
    break
  done < <(git log "$current_sha" --format='%H' -G '^APP_VERSION_CODE=' -- gradle.properties)
fi

range=""
if [[ -n "$previous_sha" ]]; then
  range="${previous_sha}..${current_sha}"
  commits=$(git log --format='%s' "$range")
else
  commits=$(git log --format='%s' "$current_sha")
fi

scope='(\([^)]*\))?!?:'
known_prefix="^(feat|feature|fix|hotfix|refactor|perf|improve|i18n|translation|translations|security|ci|chore|docs|build|test|style|deps|update|revert)${scope}"
sections_written=0

write_section() {
  local title=$1
  local pattern=$2
  local selected
  selected=$(printf '%s\n' "$commits" | grep -iE "$pattern" | sort -u || true)
  if [[ -n "$selected" ]]; then
    echo "### $title"
    printf '%s\n' "$selected" |
      sed -E 's/^[A-Za-z]+(\([^)]*\))?[!]?:[[:space:]]*//' |
      sed 's/^/- /'
    echo
    sections_written=$((sections_written + 1))
  fi
}

mkdir -p "$(dirname "$output_path")"
{
  echo "## 更新内容"
  echo
  write_section "新增功能" "^(feat|feature)${scope}"
  write_section "修复" "^(fix|hotfix)${scope}"
  write_section "优化" "^(refactor|perf|improve)${scope}"
  write_section "安全" "^security${scope}"
  write_section "翻译" "^(i18n|translation|translations)${scope}"

  other=$(printf '%s\n' "$commits" | grep -ivE "$known_prefix" | sort -u | sed '/^$/d' || true)
  if [[ -n "$other" ]]; then
    echo "### 其他用户可见变更"
    printf '%s\n' "$other" | sed 's/^/- /'
    echo
    sections_written=$((sections_written + 1))
  fi

  if [[ $sections_written -eq 0 ]]; then
    echo "- 本次发布未包含面向用户的变更。"
    echo
  fi

  if [[ -n "$previous_sha" && -n "${GITHUB_REPOSITORY:-}" ]]; then
    previous_ref=${previous_tag:-$previous_sha}
    echo "---"
    echo "完整提交记录：[查看](https://github.com/${GITHUB_REPOSITORY}/compare/${previous_ref}...${current_sha})"
    echo
  fi

  if [[ -n "$range" ]]; then
    authors=$(git log --format='%an' "$range" | sort -u | sed '/^$/d' || true)
    if [[ -n "$authors" ]]; then
      echo "### 贡献者"
      printf '%s\n' "$authors" | sed 's/^/- /'
      echo
    fi
  fi
} > "$output_path"

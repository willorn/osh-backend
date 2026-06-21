#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "AGENTS.md"
  "CONTRIBUTING.md"
  "CODE_REVIEW.md"
  "SECURITY.md"
  "docs/ai/README.md"
  "docs/ai/01-newcomer-guide.md"
  "docs/ai/02-workflow.md"
  "docs/ai/03-codex-harness.md"
  "openspec/project.md"
  "openspec/templates/proposal.md"
  "openspec/templates/design.md"
  "openspec/templates/tasks.md"
  "openspec/templates/spec.md"
  ".github/PULL_REQUEST_TEMPLATE.md"
)

for file in "${required_files[@]}"; do
  if [[ ! -s "$file" ]]; then
    echo "Missing required AI process file: $file" >&2
    exit 1
  fi
done

base_ref="${BASE_REF:-origin/${GITHUB_BASE_REF:-qa/20260708}}"

if git rev-parse --verify "$base_ref" >/dev/null 2>&1; then
  changed_files="$(git diff --name-only "$base_ref"...HEAD || true)"
else
  changed_files="$(git diff --name-only HEAD~1...HEAD || true)"
fi
untracked_files="$(git ls-files --others --exclude-standard || true)"
changed_files="$(printf '%s\n%s\n' "$changed_files" "$untracked_files" | sed '/^$/d' | sort -u)"

if echo "$changed_files" | grep -q '^openspec/changes/[^/][^/]*/'; then
  bad=0
  while IFS= read -r change_dir; do
    [[ -z "$change_dir" ]] && continue
    for file in proposal.md design.md tasks.md; do
      if [[ ! -s "$change_dir/$file" ]]; then
        echo "OpenSpec change missing $file: $change_dir" >&2
        bad=1
      fi
    done
    if ! find "$change_dir/specs" -type f -name 'spec.md' -size +0c >/dev/null 2>&1; then
      echo "OpenSpec change missing specs/**/spec.md: $change_dir" >&2
      bad=1
    fi
  done < <(echo "$changed_files" | sed -n 's#^\(openspec/changes/[^/][^/]*\)/.*#\1#p' | sort -u)
  exit "$bad"
fi

if [[ "${ALLOW_OPENSPEC_EXEMPT:-}" == "true" ]]; then
  echo "OpenSpec change not found, but ALLOW_OPENSPEC_EXEMPT=true."
  exit 0
fi

cat >&2 <<'EOF'
No OpenSpec change detected in this PR.

If this is a tiny non-behavioral change, set ALLOW_OPENSPEC_EXEMPT=true in CI only after the PR body contains:

OpenSpec-Exempt: true
Reason: <clear reason>
EOF
exit 1

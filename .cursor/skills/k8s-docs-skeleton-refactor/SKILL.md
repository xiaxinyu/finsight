---
name: k8s-docs-skeleton-refactor
description: Refactors a repository documentation skeleton to match Kubernetes-style docs conventions (Docs-as-Code, clear layering: Concepts/Tasks/Reference plus Tutorials/Setup). Separates user docs vs technical docs, standardizes file naming (lowercase-hyphen slugs), and provides a migration workflow and templates. Use when the user asks to restructure documentation, refactor docs skeleton, reorganize docs/, Kubernetes documentation style, Concepts/Tasks/Reference layering, split user docs and technical docs, 文档骨架重构, Kubernetes 文档规范, 按目录规范重构 docs, 概念/任务/参考 分层.
---

# Kubernetes-style docs skeleton refactor (Concept/Task/Reference)

## When to use this skill

Apply this skill when the user asks to:

- Refactor the repository documentation structure to a Kubernetes-style hierarchy
- Split documentation into **user docs** and **technical docs**
- Reorganize `docs/` into **Concepts / Tasks / Reference** (plus tutorials/setup)
- Enforce Task doc structure (Prerequisites/Steps/Verification/Cleanup)
- Standardize doc filenames to **lowercase-with-hyphens** slugs

Trigger keywords/phrases (EN/ZH):

- "Kubernetes docs style", "Kubernetes documentation", "Concept Task Reference"
- "refactor docs skeleton", "restructure docs", "reorganize docs/"
- "split user docs and technical docs"
- “文档规范/目录规范/文档骨架”, “Kubernetes 文档”, “概念/任务/参考”, “重构 docs”

## Goal

Refactor a repository’s documentation from a flat/sprawling layout into a Kubernetes-style layered system, while **explicitly separating user docs and technical docs**:

- User docs are organized into **Concept / Task / Tutorial / Setup** (understand → do → end-to-end examples → environment setup)
- Technical docs are centered around **Reference** (parameters/API/fields/config dictionary), with optional `architecture/` and `contributing/`

## Core conventions (abstracted from Kubernetes docs)

- **Docs as Code**: docs live with code, go through PR/Review/CI; “No doc, no merge”
- **User-oriented**: organize by “what users want to do”, not “what the system can do”
- **Clear layering**: Concept (what/why) / Task (how) / Reference (lookup)
- **Task template discipline**: intro, prerequisites, steps, verification, optional cleanup
- **File naming**: `lowercase-with-hyphens.md`, readable URL/path structure

## Target skeleton (recommended default)

> You can tailor it to the project, but do not mix responsibilities across Concept/Task/Reference.

```
docs/
  user/
    concepts/
    tasks/
    tutorials/
    setup/
  tech/
    reference/
    architecture/        # optional: design/architecture/ADR
    contributing/        # optional: contribution/dev/docs guidelines
  _index.md              # docs landing page (optional but recommended)
```

### Localization (recommended options)

Choose one. Default is A (closer to kubernetes.io’s “same structure per language” mental model):

- A: parallel language roots (recommended)

```
docs/
  en/...    (same structure)
  zh-cn/... (same structure)
```

- B: keep the same directory, differentiate via filename suffix (your repo already does this; lower migration cost)
  - e.g. `product-guide.md` and `product-guide.zh-cn.md`

If you choose B: **the same document must keep the same slug** (`product-guide`); only the language suffix differs.

## Refactor workflow (follow in order)

### 1) Inventory & classify (map existing `docs/*` into layers)

Label every existing document as one of: `concept` / `task` / `reference` / `tutorial` / `setup` / `tech-other`.

Classification rules:
- **Concept**: explain principles/design/terminology; avoid step-by-step; “what/why”
- **Task**: a concrete operation users complete; copy-pastable commands/config; step-by-step; includes verification
- **Reference**: fields/API/flags/config; strongly structured; like a dictionary/manual
- **Tutorial**: a more connected end-to-end walkthrough than a single task
- **Setup**: installation, prerequisites, bootstrap (dev/runtime environment)

Produce a migration mapping (in the PR description or a temporary file):

```text
old path -> new path | type | notes (split/merge?)
```

### 2) Create the new skeleton and a landing page

- Create directories following the target skeleton
- Create `docs/_index.md` (or `docs/en/_index.md` / `docs/zh-cn/_index.md`)
  - Must include: quick entry points (concepts/tasks/reference), a “getting started” path, and common links

### 3) Migrate & rename (keep URLs/slugs stable)

- Standardize filenames: lowercase + hyphens
- Avoid mixing Concept/Task/Reference content in one page
  - If mixed: prefer **splitting** into multiple pages and linking between them
- For externally referenced paths: keep a short-term redirect/compatibility strategy (if your docs site supports it)

### 4) Rewrite Task docs using the template (strict)

Every `docs/**/tasks/*.md` must contain the following sections (fixed order):

- Intro (1–2 sentences: what you’ll accomplish)
- Prerequisites
- Steps
- Verification
- Cleanup (optional)

See `TEMPLATES.md` for templates.

### 5) “Flexible, but not mixed” technical docs

Your intent is to keep user docs and technical docs “open/flexible” while still structured. Apply it like this:

- **User docs**: strictly layered as Concept/Task/Tutorial/Setup; written around “what you want to do”
- **Technical docs**: allow more freedom, but **Reference must remain structured**; architecture/design discussions go to `tech/architecture/` (or ADRs)
- If technical depth would interrupt the user path: keep user docs concise and link out to `tech/`

### 6) Docs-as-Code quality gates (recommended)

Minimum viable gates (recommended first):
- Markdown lint
- Link checking (internal relative links + external)
- Spelling/terminology (optional)

Principle: new features / behavior changes / config changes must update docs; no docs, no merge.

## Required outputs (when executing this skill)

You must produce:

- **The new docs skeleton tree** (text form is fine)
- **A migration mapping list** (old -> new)
- **At least one Task** rewritten using the template (pick the best candidate from existing docs)
- **A first draft landing page** (`_index.md`)

## Appendix

- Templates and writing structure: see `TEMPLATES.md`
- Migration approach for flat `docs/`: see `MIGRATION_GUIDE.md`

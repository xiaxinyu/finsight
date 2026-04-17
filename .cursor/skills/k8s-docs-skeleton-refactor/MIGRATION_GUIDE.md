## Recommended migration path: flat `docs/` → Kubernetes-style layering

Your repository currently uses a flat `docs/` layout (plus duplicated English/Chinese files). The most common migration pitfalls are:

- A single page mixes concept explanations + step-by-step operations + parameter tables (layering is blurred)
- Slug/path changes break many links after migration
- EN/ZH paths drift over time, increasing maintenance cost

Below is a low-risk migration path (recommended to split into multiple PRs):

### PR 1: introduce the skeleton and landing page only (no content moves yet)

- Create the new directory tree (`docs/user/...`, `docs/tech/...`)
- Add `docs/_index.md` (or a language root `_index.md`)
- Establish navigation first so the team aligns on the classification model

### PR 2: migrate 1–3 most “Task-like” pages first (create exemplars)

Prioritize pages that already include copy-pastable commands and verifiable outputs:

- Rename to `lowercase-with-hyphens.md`
- Strictly apply the Task template (Prerequisites/Steps/Verification/Cleanup)
- Extract concept explanations into `concepts/` and link them from the Task page

### PR 3: migrate Reference (structured lookup)

Good candidates:

- Configuration field descriptions
- API/CLI flag descriptions

The goal of Reference pages is fast lookup, so:

- Minimize narrative; maximize structure
- Each field/flag should have consistent metadata (type/required/default/description/example)

### PR 4: bulk migrate the rest (prioritize splitting)

When a page spans multiple layers, prefer splitting:

- Concept: principles/mechanisms
- Task: step-by-step procedures
- Reference: fields/API/flags

After splitting, connect them with “Next steps” / “Related links” to form the path: understand → do → lookup.

### Localization guidance (given your current duplicated EN/ZH setup)

If you don’t want a large move right now, you can keep the “same directory + language suffix” approach, but enforce these rules:

- For the same slug, EN/ZH files must live in the **same relative directory**
- Titles may differ, but link structure must remain consistent

Example:

```text
docs/user/tasks/deploy-nginx.md
docs/user/tasks/deploy-nginx.zh-cn.md
```

### Migration acceptance checklist

- Any Task page makes it easy to locate: prerequisites / steps / verification
- Any “parameter/field description” belongs in Reference
- The landing page lets a newcomer find within 30 seconds:
  - Where to learn concepts
  - Where to do tasks
  - Where to lookup reference material

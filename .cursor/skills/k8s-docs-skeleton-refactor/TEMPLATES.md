## Task template (Kubernetes style)

> Applies to: `docs/**/tasks/*.md`

```markdown
# [Task title: start with a verb, describe what you will accomplish]

[1–2 sentence intro: what you will do and when it applies]

## Prerequisites

- [Dependency 1: e.g. kubectl installed]
- [Dependency 2: e.g. access to a Kubernetes cluster]
- [Permissions/environment: e.g. write access to the target namespace]

## Steps

1. [Purpose of step 1]

   ```bash
   # Copy-pastable command
   ```

2. [Purpose of step 2]

   ```yaml
   # Copy-pastable config
   ```

## Verification

Explain how to confirm the task is complete, including copy-pastable commands and what to expect:

```bash
kubectl get ...
```

## Cleanup (optional)

If the task creates resources, provide cleanup commands:

```bash
kubectl delete ...
```
```

## Concept template

> Applies to: `docs/**/concepts/*.md`

```markdown
# [Concept title: noun/term]

[One-sentence definition: what it is]

## Why it exists

[Explain motivation/value/problem it solves]

## How it works

[Explain mechanism/architecture/key points; avoid step-by-step instructions]

## Related

- [Link to other concepts or reference pages]
```

## Reference template

> Applies to: `docs/**/reference/*.md`

```markdown
# [Reference title: object/interface/config name]

## Overview

- Scope:
- Version/compatibility:
- Defaults/behavior:

## Fields/parameters

List with a consistent structure (prefer sections or lists; avoid long paragraphs):

### `[field_or_flag_name]`

- Type:
- Required:
- Default:
- Description:
- Example:

## Examples

```yaml
# Minimal working example
```
```

## Tutorial template (end-to-end)

> Applies to: `docs/**/tutorials/*.md`

```markdown
# [Tutorial title: end-to-end goal]

## What you'll build

[1–3 sentences]

## Prerequisites

- ...

## Steps

1. ...

## Verify the result

...

## Next steps

- Link to relevant tasks/reference
```

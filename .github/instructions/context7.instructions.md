---
description: "Use when requests involve library/API documentation, code generation, setup steps, dependency configuration, framework/tooling initialization, or environment configuration. Require Context7-first workflow."
name: "Context7 First For Docs And Setup"
---
# Context7 First Workflow

- For any request involving library/API docs, code generation, project setup, or configuration steps, always use Context7 before answering.
- Resolve the library ID first with `mcp_io_github_ups_resolve-library-id` unless the user already provides a valid Context7 library ID (`/org/project` or `/org/project/version`).
- Retrieve documentation with `mcp_io_github_ups_get-library-docs` and choose mode by intent:
  - `code`: API signatures, usage patterns, and implementation snippets.
  - `info`: conceptual guidance, architecture, migration notes, and explanations.
- If the first page is insufficient, continue pagination (`page=2..10`) for the same topic before concluding missing information.
- Prefer Context7 output over memory-based recall when specific version behavior or configuration details are requested.
- If Context7 has no suitable match, explicitly state that limitation and then provide a best-effort answer with clear assumptions.
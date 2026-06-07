---
name: no-bash-build
description: Review sessions may lack a Bash tool; cannot run ./gradlew build or tests
metadata:
  type: feedback
---

Some review sessions provide no Bash/shell tool (only Read/Edit/Write/Glob/Grep/WebFetch/WebSearch).

**Why:** Without a shell, the "Compile and Test" review step cannot be executed.

**How to apply:** When no Bash tool is available, do not claim the build/tests were run. State explicitly that compilation/checkstyle/test verification was done by static analysis only and recommend the author run `./gradlew :nutrition:build` (or the relevant module) before merge. If a Bash tool IS present, run it as normal.

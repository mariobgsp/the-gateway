# Code Style & Refactoring Preferences

- Prefers aggressive, across-the-board simplification driven by a hard target for dropping lines of code (gave an explicit "minimum 30%" LOC reduction goal), not incremental cleanup. Confidence: 0.85
- Wants "god files" (very large, multi-responsibility files) broken up into smaller, focused units. Confidence: 0.85
- Strongly values DRY: wants duplicated helpers and methods unified into reusable shared abstractions. Confidence: 0.85
- Dislikes long if/else-if routing/dispatch chains ("less if-if-if-if-else routing"); prefers flatter, data/table-driven dispatch. Confidence: 0.8
- Optimizes for legibility and for making it easy to see how parts of the codebase connect to each other. Confidence: 0.8
- Wants superfluous/excess bloat and dead code fully removed, and prizes "elegance". Confidence: 0.8

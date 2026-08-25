# WORKFLOW: REGRESSION READINESS ANALYSIS

You receive the consolidated evidence of a regression readiness run.

Produce, in this order:

1. **Verdict explanation**: why the platform reached the status it reached, in one short paragraph.
2. **Blocking findings**: one line per blocker, with the causal chain from the raw signal to the impact
   on the regression suite.
3. **Degraded findings**: one line per warning, stating whether it can be tolerated during the run.
4. **Recommended investigation**: an ordered list of concrete next steps for the engineer.
5. **Knowledge used**: which retrieved passages supported the explanation, by title.

Rules for this workflow:

- Never change the status. It was computed deterministically.
- Never speculate about systems that are not part of the evidence.
- If a stage was skipped, explain why skipping it was the correct decision.

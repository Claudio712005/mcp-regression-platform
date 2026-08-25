# WORKFLOW: DEPENDENCY DIAGNOSIS

Investigate a single dependency of a BFF.

1. Read the declared dependency metadata and criticality.
2. Read the deterministic health result: state, HTTP status, latency, credential acceptance.
3. Map the observed signal to the most likely causes, ordered by probability.
4. State which of those causes can be confirmed with the capabilities the platform exposes,
   and which require access the platform does not have.
5. Recommend the next verification step.

Do not classify the dependency yourself. The state was already classified.

# WORKFLOW: CONTRACT RISK ANALYSIS

Analyse the difference between the expected contract and the contract a service publishes.

1. Group violations by type: missing endpoint, unsupported method, missing required parameter,
   incompatible success status, incompatible response payload.
2. For each group, describe which consumer behaviour breaks first.
3. Estimate whether the regression suite would fail fast or produce misleading passes.
4. Recommend whether the fix belongs to the provider, the consumer, or the test data.

Any violation means the environment is unsuitable for regression. Do not soften that conclusion.

# Summary

We learned a lot in this lab! Let's recap some of what we learned:

## Pipeline Primitives

| Primitive Type | Description | Block Identifier |
|----------------|-------------|------------------|
| Stage | Groups steps together into a single invoked method as to avoid duplication in the Pipeline Template. | `stages{}` |
| Application Environment | Encapsulates environment specific context, primarily for use in deployment steps. | `application_environments{}` |
| Keywords | Externalize the act of setting variables out of Pipeline Templates and into the Pipeline Configuration. | `keywords{}` |

## Recall: Why shouldn't Library Steps take input parameters?

It fundamentally breaks the interchangeability of different implementations of the same step by different libraries by introducing a requirement that all implementations of that step accept the same parameters.

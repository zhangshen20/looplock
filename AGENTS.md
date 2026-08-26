# Repository Rules

- Preserve the safety boundary: deterministic Android code enforces local rules. AI output can never unlock, allow, delete, shorten, disable, or directly enforce a rule.
- Do not add accessibility window-content retrieval, broad installed-app visibility, screenshots, typed-text collection, financial data, covert monitoring, or third-party gambling integrations.
- Keep all demo fixtures harmless, local, and clearly labeled. They must not contain gambling mechanics, payments, accounts, ads, analytics, network access, or third-party assets.
- Do not deploy, enable Google Cloud APIs, change IAM, create paid resources, publish, contact third parties, or submit to Devpost without explicit user approval.
- Never commit credentials, service-account keys, local Cloud configuration, recordings containing private information, or raw user activity.
- Run the monotonic-policy and contract tests after any rule-schema or enforcement change.
- Keep the private Cloud Run proxy labeled as demo-only infrastructure; it is not the production mobile-authentication design.


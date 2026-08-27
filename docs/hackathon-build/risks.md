# Risk Register

Scales: likelihood and impact are Low, Medium, or High. Owners should update triggers daily until the internal submission target.

## Product and Safety Risks

| Risk | Likelihood | Impact | Mitigation | Trigger / response |
| --- | --- | --- | --- | --- |
| User mistakes the beta for treatment or guaranteed harm prevention | Medium | High | Application-ready beta copy, supported-environment limits, support disclaimer, no clinical claims | If copy says “prevents” or “treats,” block release and rewrite |
| Coercive use by partner, parent, employer, or clinician | Medium | High | Adult self-use target, local consent, no remote setup, no covert mode | If a control can be configured without device-user action, remove it |
| False positive quarantines a legitimate new app | High | Medium | Strict option off by default, clear consequence, `REVIEW` state, short demo commitment | If production framing implies guilt, change to neutral quarantine language |
| User feels trapped or distressed | Medium | High | Exact end time, delayed recovery path, five-minute demo duration, no indefinite policy | If expiry cannot be trusted, do not demo active commitment |
| Accountability leaks sensitive behavior | Medium | High | Preview only; counts and time window; no targets; no contacts | If any message is actually sent in MVP, cut feature pending consent design |
| Agent incorrectly classifies an app | Medium | Medium | Quarantine precedes classification; agent never unlocks; unknown => `REVIEW` | If confidence is low or metadata incomplete, keep quarantine and label uncertain |

## Android and Enforcement Risks

| Risk | Likelihood | Impact | Mitigation | Trigger / response |
| --- | --- | --- | --- | --- |
| Accessibility block is visibly late or inconsistent | High | High | Test first on one emulator/API level and two demo packages; foreground service status | If unreliable by 22 Aug, pivot demo to controlled launcher/test surface and disclose limitation |
| User revokes service, force-stops, uninstalls, uses safe mode, or another profile | High | High | State consumer limits explicitly; show `Action required`; never claim tamper-proof | Do not attempt to block settings or uninstall in consumer mode |
| Package install event is missed | Medium | High | Register receiver correctly, reconcile targeted demo package on resume, add diagnostics | If event misses twice in 10 runs, make reconciliation path P0 |
| Clock manipulation shortens commitment | Medium | High | Combine persisted wall time with monotonic elapsed time for demo; test reboot/rollback | If rollback expires early, block release until fixed or remove time-bound claim |
| Site blocking expands scope and breaks traffic | High | High | P2 two-hour spike only after app loop passes | Stop immediately at time box or first device-wide regression |
| Broad app visibility causes privacy/Play issues | Medium | High | Target only known demo packages; do not request `QUERY_ALL_PACKAGES` | Manifest review detects broad permission => remove before demo |

## Play Policy and Distribution Risks

| Risk | Likelihood | Impact | Mitigation | Trigger / response |
| --- | --- | --- | --- | --- |
| Accessibility use is rejected or requires additional review | High | High | Treat APK as an application-ready sideloaded beta; prominent disclosure; declaration plan; no deceptive automation | Never claim Play readiness in submission |
| `VpnService` use does not fit a permitted core purpose | Medium | High | Keep out of P0; document firewall rationale and declaration requirements | Do not include VPN in APK unless the spike is defensible |
| Accessibility is interpreted as autonomous execution | Medium | High | Agent never drives accessibility actions; deterministic service only reads package/window events and redirects based on local rules | Architecture review must show no agent-to-AccessibilityService command path |
| Store metadata overstates control | Medium | High | Use exact consumer-mode language and screenshots | Any “cannot be bypassed” claim blocks release |

## Privacy and Security Risks

| Risk | Likelihood | Impact | Mitigation | Trigger / response |
| --- | --- | --- | --- | --- |
| Package names reveal sensitive interests | Medium | High | Explicit consent; transient raw metadata; hash in Firestore; no account | Raw package name in persistent cloud store => fail privacy acceptance test |
| Cloud endpoint is abused and incurs cost | Medium | Medium | Auth/rate limits, max instances, budget alerts, scale to zero, disable after proof | Unexpected requests or cost => disable endpoint and use recorded proof |
| Secret leaks from APK/repo | Medium | High | Server-side credentials only, environment configuration, secret scanning | Any secret finding blocks repository publication |
| Prompt injection in app label/metadata | Medium | High | Treat metadata as untrusted, schema-constrained output, local monotonic validator | Malicious label produces non-schema action => validator test must reject |
| Firestore records become a behavioral log | Medium | High | Short retention, anonymous IDs, minimal fields, no raw timeline | Field inventory exceeds documented schema => remove before demo |

## Hackathon Delivery Risks

| Risk | Likelihood | Impact | Mitigation | Trigger / response |
| --- | --- | --- | --- | --- |
| Empty repository leaves too much build work | High | High | Risk-first spike; freeze scope; daily demoable increment | P0 enforcement not working by 22 Aug => cut all P1/P2 |
| Agent looks ornamental | Medium | High | Make package event drive classification and additive rule; show real logs | If demo still works identically without agent, refine event/result or state limitation |
| Project looks like a blocker, not an agent | Medium | High | Emphasize multi-step event workflow and autonomous routing, not chat | Demo must show event -> Cloud -> rule -> offline enforcement |
| Video exceeds four minutes or hides proof | Medium | High | Script now, record 26 Aug, show real action early | First cut >4:00 => remove setup/loading, not core proof |
| Missing reproducibility, diagram, or cloud evidence | Medium | High | Treat each as P0 and draft during build | Any missing item on 27 Aug => stop feature work |
| Start-date/originality ambiguity | Low | High | Record 19 Aug project creation; disclose templates and any reused assets/code | Any pre-existing code added => document provenance before submission |
| Final-hours upload or processing failure | Medium | High | Submit by 28 Aug; upload public video early; use final deadline only as buffer | No submitted draft by 28 Aug => daily submission-readiness review |

## Consumer Mode vs Managed-Device Strong Mode

### Consumer mode — hackathon MVP

- Normal personal-device installation.
- User-granted accessibility permission and potentially VPN consent.
- Can redirect/block selected packages while services are active.
- Cannot honestly prevent permission revocation, force-stop, uninstall, safe mode, secondary profiles, or all OEM-specific bypasses.
- Play distribution is subject to sensitive-API declarations and review.

### Managed-device strong mode — production research

- Device-owner or appropriate profile-owner provisioning.
- Can suspend packages, block uninstall, and enforce always-on VPN policies within the controlled scope.
- A personal work profile controls its profile, not necessarily the entire personal side.
- Fully managed device setup is operationally heavy and creates coercion/governance risks.
- Not built or claimed in this submission.

## Safety Release Principle

If forced to choose between a broader demo and an honest boundary, choose the boundary. A blocked-but-reviewable false positive is acceptable for a five-minute, explicitly strict demo commitment; an AI-created unlock path is not.

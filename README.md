# cloud-itonami-isco-7411

Open Occupation Blueprint for **ISCO-08 7411**: Building and Related Electricians.

This repository designs a forkable OSS business for an independent licensed electrician: a wiring-inspection robot performs panel scanning and continuity testing under a governor-gated actor, so the practice keeps its own inspection and repair records instead of renting a closed field-service SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a wiring-inspection robot performs panel scanning, continuity testing and thermal-imaging checks under an actor that proposes
actions and an independent **Electrical Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating on live circuits, or panel access without lockout) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
service request + wiring diagram + safety lockout requirement
        |
        v
Electrical Advisor -> Electrical Governor -> wire/test, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7411`). Required capabilities:

- :robotics
- :forms
- :telemetry
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/electrical_practice/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `electrical-practice.store` — `Store` protocol + `MemStore`:
  registered jobs (with an `is-live-work?` flag), circuit tests, permits.
  A test/permit can only be recorded against a registered job (job
  provenance).
- `electrical-practice.governor` — `ElectricalPracticeGovernor`: `assess`
  gates a proposal against the job env. Hard invariants force `:hold`
  (no job, direct-write instead of `:propose`, or any proposal on a
  live-work job below `:high` safety-class); any proposal on a
  `is-live-work?` job always requires `:high`+ safety-class and thus
  `:human-approval` — it can never be auto-approved; low-confidence
  proposals also escalate.

```bash
clojure -M:test   # 7 tests, 12 assertions, green
```

This repo's own `blueprint.edn` currently declares `:itonami.blueprint/maturity
:blueprint`, not `:implemented`: `store`/`governor` are real, but there is no
compiled `langgraph-clj` StateGraph, Advisor protocol, or audit ledger wired
around them yet, so the actor cannot yet run an end-to-end proposal ->
governor -> commit/hold cycle. Any `:implemented`-tier listing for this repo
in [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
predates that correction and should be treated as stale until the missing
StateGraph/Advisor/ledger layer is built.

## License

AGPL-3.0-or-later.

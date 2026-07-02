(ns electrical-practice.governor
  "ElectricalPracticeGovernor — the independent safety/traceability layer
  for the ISCO-08 7411 independent electrical actor. The Job Advisor
  proposes actions (circuit-test, permit); it has no notion of job
  provenance or live-work risk, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD — the itonami-actor pattern
  (independent Governor gates a proposing actor) applied to this
  occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. Any circuit-test or permit
  action on a job marked `is-live-work?` ALWAYS requires human sign-off
  — it can never be auto-approved.

  HARD invariants for :electrical-practice/propose:
    1. Job provenance    — a circuit-test or permit must reference a
       registered job.
    2. No-actuation       — the proposal must not directly mutate a
       circuit-test/permit record outside the record-circuit-test!/
       record-permit! path (effect must be :propose, never a raw store
       write).
    3. Live-work safety  — any proposal on a job with `is-live-work?`
       true always requires :high or higher safety-class, forcing human
       sign-off; it is never auto-approved regardless of confidence.
  SOFT:
    4. Confidence floor → escalate."
  (:require [electrical-practice.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- hard-violations [{:keys [job-fn]} proposal]
  (let [{:keys [job-id safety-class effect]} proposal
        found-job (job-fn job-id)]
    (cond-> []
      (nil? found-job)
      (conj {:rule :no-job :detail (str "未登録 job " job-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and found-job (:is-live-work? found-job)
           (< (safety-rank (or safety-class :none)) (safety-rank :high)))
      (conj {:rule :live-work-safety
             :detail "is-live-work? な job への操作は :high 以上の safety-class が必須"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:job-fn` lookup,
  decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 1.0)]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `electrical-practice.store/Store` implementation."
  [store]
  {:job-fn #(store/job store %)})

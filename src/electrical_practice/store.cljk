(ns electrical-practice.store
  "SSoT for the ISCO-08 7411 independent electrical sole-proprietor
  actor, behind a `Store` protocol so the backend is a swap (MemStore
  default ‖ a real Datomic/kotoba-server backend, per the itonami actor
  pattern).

  Domain = independent electrical practice:

    job              — a registered job (jobId, address, isLiveWork?
                       boolean — true if the circuit will not be
                       de-energized during work)
    circuit-test     — a circuit test event under a job (testId, jobId,
                       reading)
    permit           — a permit event under a job (permitId, jobId,
                       status #{:filed :approved :closed})

  The append-only records are the operating ledger: a circuit-test or
  permit must reference a registered job, and these records are never
  mutated in place, only appended.")

(defprotocol Store
  (job [st job-id])
  (circuit-tests-of [st job-id])
  (permits-of [st job-id])
  (register-job! [st job])
  (record-circuit-test! [st circuit-test])
  (record-permit! [st permit]))

(defrecord MemStore [state]
  Store
  (job [_ job-id]
    (get-in @state [:jobs job-id]))
  (circuit-tests-of [_ job-id]
    (filter #(= job-id (:job-id %)) (:circuit-tests @state)))
  (permits-of [_ job-id]
    (filter #(= job-id (:job-id %)) (:permits @state)))
  (register-job! [_ job]
    (swap! state assoc-in [:jobs (:job-id job)] job))
  (record-circuit-test! [_ circuit-test]
    (swap! state update :circuit-tests (fnil conj []) circuit-test))
  (record-permit! [_ permit]
    (swap! state update :permits (fnil conj []) permit)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:jobs {} :circuit-tests [] :permits []} seed)))))

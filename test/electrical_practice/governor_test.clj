(ns electrical-practice.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [electrical-practice.store :as store]
            [electrical-practice.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-job! st {:job-id "job-1" :address "1 Main St" :is-live-work? false})
    (store/register-job! st {:job-id "job-2" :address "2 Oak St" :is-live-work? true})
    st))

(deftest proceeds-on-clean-circuit-test
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :circuit-test :job-id "job-1" :safety-class :low
                   :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-job
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :circuit-test :job-id "no-such-job" :safety-class :low
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-job (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :circuit-test :job-id "job-1" :safety-class :low
                   :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest holds-on-live-work-job-without-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :circuit-test :job-id "job-2" :safety-class :medium
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :live-work-safety (:rule %)) (:violations result)))))

(deftest human-approval-on-live-work-job-with-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :permit :job-id "job-2" :safety-class :high
                   :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :circuit-test :job-id "job-1" :safety-class :none
                   :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-circuit-test! st {:test-id "t1" :job-id "job-1" :reading "120V"})
    (store/record-permit! st {:permit-id "p1" :job-id "job-1" :status :filed})
    (is (= 1 (count (store/circuit-tests-of st "job-1"))))
    (is (= 1 (count (store/permits-of st "job-1"))))))

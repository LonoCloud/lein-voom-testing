(ns lein-voom-testing.core-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]))

(defn select-item
  [coll selector]
  (when-not (zero? (count coll))
    (nth coll (mod selector (count coll)))))

(def new-project-repo-gen (gen/tuple (gen/return :new-project-repo) gen/string-alphanumeric))
(defn new-project-repo
  [state name]
  (-> state
      (assoc-in [:state name] {})
      (update-in [:actions] conj [:new-project-repo name])))

(def new-agent-gen (gen/tuple (gen/return :new-agent) gen/string-alphanumeric gen/nat))
(defn new-agent
  [state name repo-select]
  (let [repos (keys (:state state))
        repo (select-item repos repo-select)]
    (if repo
      (-> state
          (assoc-in [:state repo] {name :details})
          (update-in [:actions] conj [:new-agent name repo]))
      ;; NOOP because there is no repo
      state)))

(defn apply-actions
  [state [action & args]]
  (case action
    :new-project-repo (apply new-project-repo state args)
    :new-agent (apply new-agent state args)))

(defn do-run [n]
  ;; I could seed with an initial repo but I specifically want to show
  ;; how the new-agent can be NOOPed when there there is no repo yet
  ;; -- this will come in to play more with other generators.
  (map #(reduce apply-actions {:state {} :actions []} %)
       (gen/sample (gen/vector
                    (gen/frequency [[10 new-project-repo-gen]
                                    [90 new-agent-gen]]))
                   n)))

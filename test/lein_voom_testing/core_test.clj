(ns lein-voom-testing.core-test
  (:require [clojure.spec :as spec]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [datascript.core :as ds]))

"

"

(spec/def ::test-run (spec/keys :req [::state ::actions]))
(spec/def ::state map?)
(spec/def ::action #(and (vector? %) (keyword? (first %))))
(spec/def ::new-project-repo-action (spec/cat :action #{:new-project-repo}))
(spec/def ::new-agent-action (spec/cat :action #{:new-agent}
                                       :repo-selector long?))
(spec/def ::actions (spec/coll-of ::action []))

(defn select-item
  [coll selector]
  (when-not (zero? (count coll))
    (nth coll (mod selector (count coll)))))

;; Multimethod definitions

(defmulti action-generator
  "Returns a generator of un-resolved actions."
  identity)

(defn action-fn
  [run [action & args]]
  action)

(defmulti resolve-action
  "Resolves an un-resolved action against an accumulated state."
  #'action-fn)

(defmulti do-action
  "Applies the side-effects of a resolved action."
  #'action-fn)

(defmethod do-action :default [& args]
  (prn "Unimplemented" args))

;; :new-project-repo

(defmethod action-generator :new-project-repo [_]
  (gen/tuple
   (gen/tuple (gen/return :new-project-repo))))

(defmethod resolve-action :new-project-repo
  [run [action]]
  (let [n (count (::state run))
        repo-name (str "repo-" n)]
    (-> run
        (assoc-in [::state repo-name] {})
        (update-in [::actions] conj [:new-project-repo repo-name]))))

;; :new-agent

(defmethod action-generator :new-agent [_]
  (gen/tuple
   (gen/tuple (gen/return :new-agent) gen/nat)))

(defmethod resolve-action :new-agent
  [run [action repo-select]]
  (let [repos (keys (::state run))]
    (if (empty? repos)
      ;; NOOP because there is no repo
      (update-in run [::actions] conj [::noop])
      (let [repo (select-item repos repo-select)
            agents (-> run ::state (get repo))
            agent-name (str repo "-agent-" (count agents))]
        (-> run
            (update-in [::state repo] conj [agent-name ::details])
            (update-in [::actions] conj [:new-agent agent-name repo]))))))

(defn do-run [n]
  (map #(reduce resolve-action
                {::state {"repo-0" {}}
                 ::actions [[:new-project-repo "repo-0"]]}
                %)
       (apply concat
              (gen/sample (gen/vector
                           (gen/frequency [[ 5 (action-generator :new-project-repo)]
                                           [95 (action-generator :new-agent)]]))
                          n))))

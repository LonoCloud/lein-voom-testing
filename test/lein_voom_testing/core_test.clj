(ns lein-voom-testing.core-test
  (:require [clojure.java.shell :as shell]
            [clojure.spec :as spec]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [datascript.core :as ds]))

(def actions
  ;; global actions
  [[:new-project-repo] #_repo
   [:new-author ::repo] #_author
   ;; author actions
   [:new-project ::author] #_project
   [:new-branch ::author] #_branch
   [:commit-switch-branch ::author]
   [:git-push ::author]
   [:git-pull ::author]
   ;; author-project actions
   [:prj-run ::author ::project]
   [:prj-modify ::author ::project]
   [:prj-incr-inc ::author ::project]
   [:prj-minor-inc ::author ::project]
   [:prj-major-inc ::author ::project]
   [:prj-incr-dec ::author ::project]
   [:prj-minor-dec ::author ::project]
   [:prj-major-dec ::author ::project]
   [:clj-modify ::author ::project]
   [:git-pull-merge-modify ::author ::project]
   [:freshen-project ::author ::project]])

(spec/def ::repo (spec/spec #() :gen (gen/tuple (gen/return :repo) pos-long?)))

(spec/def ::new-project-repo-action (spec/cat :action #{:new-project-repo}))
(spec/def ::new-agent-action (spec/cat :action #{:new-agent}
                                       :repo-selector (::repo))

  (spec/def ::action (spec/alt :new-agent ::new-agent-action
                               :new-project-repo ::new-project-repo-action)))

(spec/def ::actions (spec/coll-of ::action []))

(spec/def ::state map?)

(spec/def ::test-run (spec/keys :req [::state ::actions]))

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
  (prn "Unimplemented action" args))

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

(def basic-seed
  [[:new-repo "repo0"]
   [:new-agent "agent0" ["project0"]]
   [:new-project "agent0" "repo0" "dir0" "project0" []]
   [:new-repo "repo1"]
   [:new-project "repo1" "dir1" "project1" ["project0"]]
   [:new-agent "agent1" ["project1"]]])

;; src commit
;; pull/merge
;; pull/merge / push
;; build-deps
;; freshen
;; run - later

;; infra directory (seed projects and maven repo)
;; agent directories (WC, .m2 .voom-repos)

;; NOTE: A new-dep adds to the existing deps of child proj and points
;; to current version of parent proj (for now).

(defn cmd [& args]
  (prn :cmd args)
  #_(apply shell/sh args))

(defmethod do-action :new-repo
  [infra-dir repo-name]
  (cmd "git" "init" "--bare" (str repo-name ".git" :dir infra-dir)))

#_
(defmethod do-action :new-agent
  []
  (let [src-repo ""
        agent-repo-dir ""]
      (cmd "git" "clone" )))

#_
(defmethod do-action :new-project
  []
  )

(def testing-ns "lein-voom-testing")

(defn generate-project-file
  [project-id proj-version deps]
  (let [lein-deps deps]
    `(~'defproject ~(symbol project-id) ~proj-version
       :dependencies [~@lein-deps])))

(defn generate-source-file
  "Generates standard Clojure source file to print inclusion of all dependencies.
  Tracks specific versions by edit-markers."
  [project-id edit-marker deps]
  (if (empty? deps)
    `[(ns project-id)
      (defn edit-marker-tree [] {:dep project-id :marker edit-marker})
      (defn -main [] (prn (edit-marker-tree)))]
    (let [req-deps (map #(vector (symbol testing-ns %)) deps)]
      `[(ns project-id
          (:require req-deps))

        (def edit-marker edit-marker)

        (defn edit-marker-tree []
          (let [data {:dep project-id
                      :marker edit-marker}]
            (if (empty?))))

        (defn -main []
          (prn (edit-marker-tree)))])))

(defn do-run [n]
  (let [initial-state {::state {"repo-0" {}}
                       ::actions [[:new-project-repo "repo-0"]]}]
    (map #(reduce resolve-action initial-state %)
         (apply concat
                (gen/sample (gen/vector
                             (gen/frequency [[ 5 (action-generator :new-project-repo)]
                                             [95 (action-generator :new-agent)]]))
                            n)))))

;; use gen/no-shrink
;; shrink raw->reified actions, pruning any hard dependencies
;; shrink starts at beginning, testing each action that's not pruned by an earlier shrink
;; shrink events mark subsequents as NOOPs? (or should it drop them?)
;; shrink returns #dead[#] objects to downstream
;; subsequent actions are scanned by framework to find #dead inputs
;;   and automatically return #dead outputs


;; Layers
;; raw actions - natural numbers for contingent values
;; resolved actions - modularly applied numbers select contingent values
;;  - resolved actions shouldn't need global context to apply
;;  - testing actions, however, need state-up-to-now to validate

"
Each generation:
[[:raw-action ...] [:raw-action ...]...]
-> resolve-action ->
{:
* reduce those actions against initial state to produce final-state and resolved actions
* take 


"

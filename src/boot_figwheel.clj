(ns boot-figwheel
  {:boot/export-tasks true}
  (:require [clojure.java.io :as io]
            [boot.pod :as pod]
            [boot.util :as util]
            [boot.core :as core :refer [deftask tmp-dir!]]
            [boot.task.built-in :refer [repl]]
            [boot.from.backtick :refer [template]]
            [environ.core]))

(def ^:private fw-pod (volatile! nil))
(def ^:private fw-config (volatile! nil))

(def ^:private deps
  (delay (remove pod/dependency-loaded? '[[figwheel-sidecar "0.3.7" :exclusions [cider/cider-nrepl]]])))

(defn run-figwheel []
  (let [pod-env (update (core/get-env) :dependencies into @deps)
        pod (future (pod/make-pod pod-env))
        fw-config (assoc-in @fw-config [:builds 0 :source-paths] (vec (core/get-env :source-paths)))]
    (pod/with-eval-in @pod
      (require '[figwheel-sidecar.core :refer [start-server]]
               '[figwheel-sidecar.auto-builder :refer [autobuild*]]
               '[clojurescript-build.auto :refer [stop-autobuild!]]
               '[environ.core])
      (alter-var-root (var environ.core/env) (fn [_] ~environ.core/env))
      (def +fw-server+ (start-server (:figwheel-server ~fw-config)))
      (def +fw-config+ (assoc ~fw-config :figwheel-server +fw-server+))
      (def +fw-builder+ (autobuild* +fw-config+)))
    (println "Figwheel pod has been created.")
    (vreset! fw-pod @pod)))

(defn update-figwheel []
  (pod/with-eval-in @fw-pod
    (alter-var-root (var environ.core/env) (fn [_] ~environ.core/env))))

(defn stop-figwheel []
  (pod/with-eval-in @fw-pod (stop-autobuild! +fw-builder+)))

(defn start-figwheel
  ([] (pod/with-eval-in @fw-pod (def +fw-builder+ (autobuild* +fw-config+))))
  ([config] (pod/with-eval-in @fw-pod
              (def +fw-config+ (assoc ~config :figwheel-server +fw-server+))
              (def +fw-builder+ (autobuild* +fw-config+)))))

(deftask figwheel
  [f figwheel-config FWCONFIG edn "Figwheel task configuration."]
  (vreset! fw-config figwheel-config)
  identity)

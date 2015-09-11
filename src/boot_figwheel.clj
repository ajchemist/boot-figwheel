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
  (delay (remove pod/dependency-loaded? '[[figwheel-sidecar "0.3.9" :exclusions [cider/cider-nrepl]]])))

(defn run-figwheel []
  (let [pod-env (update (core/get-env) :dependencies into @deps)
        pod (future (pod/make-pod pod-env))
        fw-config (assoc-in @fw-config [:builds 0 :source-paths] (vec (core/get-env :source-paths)))]
    (util/info "Make a fresh Figwheel pod...\n")    
    (pod/with-eval-in @pod
      (require '[figwheel-sidecar.core :refer [start-server stop-server]]
               '[figwheel-sidecar.auto-builder :refer [autobuild*]]
               '[clojurescript-build.auto :refer [stop-autobuild!]]
               '[environ.core])
      (alter-var-root (var environ.core/env) (fn [_] ~environ.core/env))
      (defonce fwp-server (start-server (:figwheel-server ~fw-config)))
      (def fwp-config (volatile! (assoc ~fw-config :figwheel-server fwp-server)))
      (def fwp-builder (volatile! (autobuild* @fwp-config))))
    (vreset! fw-pod @pod)))

(defn stop-figwheel []
  (pod/with-eval-in @fw-pod
    (alter-var-root (var environ.core/env) (fn [_] ~environ.core/env))
    (stop-autobuild! @fwp-builder)))

(defn start-figwheel
  ([] (pod/with-eval-in @fw-pod
        (vreset! fwp-builder (autobuild* @fwp-config))))
  ([config]
   (pod/with-eval-in @fw-pod 
     (vreset! fwp-config (assoc ~config :figwheel-server fwp-server))
     (vreset! fwp-builder (autobuild* @fwp-config)))))

(defn destroy-figwheel []
  (util/info "Stop Figwheel httpkit server...\n")
  (pod/with-eval-in @fw-pod (stop-server fwp-server))
  (util/info "Destroy Figwheel pod...\n")
  (pod/destroy-pod @fw-pod))

(deftask figwheel
  [f figwheel-config FWCONFIG edn "Figwheel task configuration."]
  (vreset! fw-config figwheel-config)
  identity)

(ns boot-figwheel
  {:boot/export-tasks true}
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.repl :refer [doc]]
            [boot.util :as util]
            [boot.file :as file]
            [boot.core :as boot :refer [deftask]]))

(defmacro ^:private r [sym] `(resolve '~sym))

(def ^:private deps
  '[[figwheel-sidecar "0.5.2" :scope "test"]
    [com.cemerick/piggieback "0.2.1" :scope "test"]
    [org.clojure/tools.nrepl "0.2.12" :scope "test"]])

(defn- assert-deps
  "Advices user to add direct deps to requires deps if they
  are not available."
  []
  (let [current (->> (boot/get-env :dependencies) (map first) set)
        missing (->> deps (remove (comp current first)))]
    (if (seq missing)
      (util/warn
       (str "You are missing necessary dependencies for boot-figwheel.\n"
            "Please add the following dependencies to your project:\n"
            (str/join "\n" missing) "\n\n")))))

(defn- add-boot-source-paths [{:keys [all-builds] :as options}]
  (assoc options :all-builds
         (mapv (fn [build]
                 (update build :source-paths
                         #(vec (into (boot/get-env :source-paths) %))))
               all-builds)))

(defn- check-build-output-to [build]
  (let [{id :id}    build
        target-path (boot/get-env :target-path)]
    (update-in build [:compiler :output-to]
               #(.getPath (io/file target-path (if (string? %) % (str id ".js")))))))

(defn- check-build-output-dir [build]
  (let [{id :id}   build
        target-path (boot/get-env :target-path)
        output-to   (get-in build [:compiler :output-to])
        parent      (file/parent output-to)
        output-dir  (get-in build [:compiler :output-dir])
        output-dir  (if (string? output-dir)
                      (io/file target-path output-dir)
                      (io/file parent (str id ".out")))
        asset-path  (get-in build [:compiler :asset-path])
        asset-path  (if (string? asset-path)
                      (io/file asset-path)
                      (file/relative-to target-path output-dir))]
    (-> build
      (assoc-in [:compiler :output-dir] (.getPath output-dir))
      (assoc-in [:compiler :asset-path] (.getPath asset-path)))))

(defn- check-output-path [options]
  (update options :all-builds
          (fn [all-builds]
            (mapv #(-> %
                     check-build-output-to
                     check-build-output-dir)
                  all-builds))))

(deftask figwheel "Figwheel interface for Boot repl"
  [b build-ids        BUILD_IDS [str] "Figwheel build-ids"
   c all-builds       ALL_BUILDS edn  "Figwheel all-builds compiler-options"
   o figwheel-options FW_OPTS    edn  "Figwheel options"]
  (assert-deps)
  (boot/task-options! figwheel *opts*)
  (util/info "Require figwheel-sidecar.system just-in-time...\n")
  (require '[figwheel-sidecar.system :as fs]
           '[com.stuartsierra.component :as component])
  identity)

(defn task-options []
  (-> #'figwheel
    meta :task-options
    (select-keys [:build-ids :all-builds :figwheel-options])
    add-boot-source-paths
    check-output-path))

(def ^:dynamic *boot-figwheel-system* nil)

(defn start-figwheel!
  "If you aren't connected to an env where fighweel is running already,
  this method will start the figwheel server with the passed in build info."
  []
  (when *boot-figwheel-system*
    (alter-var-root #'*boot-figwheel-system* (r component/stop)))
  (alter-var-root #'*boot-figwheel-system*
    (fn [_]
      ((r fs/start-figwheel!) (task-options)))))

(defn stop-figwheel!
  "If a figwheel process is running, this will stop all the Figwheel autobuilders and stop the figwheel Websocket/HTTP server."
  []
  (when *boot-figwheel-system*
    (alter-var-root #'*boot-figwheel-system* (r component/stop))))

(defn- figwheel-running? []
  (or (get-in *boot-figwheel-system* [:figwheel-system :system-running] false)
      (do
        (println "Figwheel System not itnitialized.\nPlease start it with boot-figwheel/start-figwheel!")
        nil)))

(defn- app-trans
  ([func ids]
   (when (figwheel-running?)
     (let [system (get-in *boot-figwheel-system* [:figwheel-system :system])]
       (reset! system (func @system ids)))))
  ([func]
   (when (figwheel-running?)
     (let [system (get-in *boot-figwheel-system* [:figwheel-system :system])]
       (reset! system (func @system))))))

(defn build-once
  "Compiles the builds with the provided build ids
(or the current default ids) once."
  [& ids]
  (app-trans (r fs/build-once) ids))

(defn clean-builds
  "Deletes the compiled artifacts for the builds with the provided
build ids (or the current default ids)."
  [& ids]
  (app-trans (r fs/clean-builds) ids))

(defn stop-autobuild
  "Stops the currently running autobuild process."
  [& ids]
  (app-trans (r fs/stop-autobuild) ids))

(defn start-autobuild
  "Starts a Figwheel autobuild process for the builds associated with
the provided ids (or the current default ids)."
  [& ids]
  (app-trans (r fs/start-autobuild) ids))

(defn switch-to-build
  "Stops the currently running autobuilder and starts building the
builds with the provided ids."
  [& ids]
  (app-trans (r fs/switch-to-build) ids))

(defn reset-autobuild
  "Stops the currently running autobuilder, cleans the current builds,
and starts building the default builds again."
  []
  (app-trans (r fs/reset-autobuild)))

(defn reload-config
  "Reloads the build config, and resets the autobuild."
  []
  (app-trans (r fs/reload-config)))

(defn print-config
  "Prints out the build configs currently focused or optionally the
  configs of the ids provided."
  [& ids]
  ((r fs/print-config)
   @(get-in *boot-figwheel-system* [:figwheel-system :system])
   ids)
  nil)

(defn cljs-repl
  "Starts a Figwheel ClojureScript REPL for the provided build id (or
  the first default id)."
  ([] (cljs-repl nil))
  ([id]
   (when (figwheel-running?)
     ((r fs/cljs-repl) (:figwheel-system *boot-figwheel-system*) id))))

(defn fig-status
  "Display the current status of the running Figwheel system."
  []
  (app-trans (r fs/fig-status)))

(defn api-help
  "Print out help for the Figwheel REPL api"
  []
  (doc cljs-repl)
  (doc fig-status)
  (doc start-autobuild)
  (doc stop-autobuild)
  (doc build-once)
  (doc clean-builds)
  (doc switch-to-build)
  (doc reset-autobuild)
  (doc reload-config)
  (doc api-help))

(ns boot-figwheel
  {:boot/export-tasks true}
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as str]
   [clojure.repl :refer [doc]]
   [boot.core :as boot :refer [deftask]]
   [boot.util :as util]
   [boot.file :as file]))

(defmacro ^:private r [sym] `(resolve '~sym))

(def ^:private deps
  '[[figwheel-sidecar "0.5.x" :scope "test"]
    [com.cemerick/piggieback "0.2.1" :scope "test"]
    [org.clojure/tools.nrepl "0.2.12" :scope "test"]])

(defn- assert-deps
  "Advices user to add direct deps to requires deps if they
  are not available."
  []
  (let [current (->> (boot/get-env :dependencies) (map first) set)
        missing (->> deps (remove (comp current first)))]
    (when (seq missing)
      (util/warn
       (str "You are missing necessary dependencies for boot-figwheel.\n"
            "Please add the following dependencies to your project:\n"
            (str/join "\n" missing) "\n\n")))))

(declare task-options)

(defn- update-build-output-to
  [{id :id :as build}]
  (let [target-path (:target-path (task-options))]
    (assert (string? target-path))
    (-> build
      (update-in [:compiler :output-to]
        (fn [out]
          (let [out (if (string? out) out (str id ".js"))]
            (.getPath (jio/file target-path out))))))))

(defn- update-build-source-map [build]
  (let [source-map (get-in build [:compiler :source-map])]
    (if (string? source-map)
      (let [target-path (:target-path (task-options))
            output-to   (get-in build [:compiler :output-to])
            parent      (file/parent output-to)]
        (-> build
          (assoc-in [:compiler :source-map]
            (.getPath (jio/file parent source-map)))
          (assoc-in [:compiler :source-map-path]
            (.getPath (file/relative-to target-path parent)))))
      build)))

(defn- update-build-output-dir
  [{id :id :as build}]
  (let [target-path (:target-path (task-options))
        output-to   (get-in build [:compiler :output-to])
        parent      (file/parent output-to)
        output-dir  (get-in build [:compiler :output-dir])
        output-dir  (if (string? output-dir)
                      (jio/file parent output-dir)
                      (jio/file parent (str id ".out")))
        asset-path  (get-in build [:compiler :asset-path])
        asset-path  (if (string? asset-path)
                      (jio/file asset-path)
                      (file/relative-to target-path output-dir))]
    (-> build
      (assoc-in [:compiler :output-dir] (.getPath output-dir))
      (assoc-in [:compiler :asset-path] (.getPath asset-path)))))

(defn- update-output-path [options]
  (update options :all-builds
          (fn [all-builds]
            (mapv
             (fn [build]
               (-> build
                 (update-build-output-to)
                 (update-build-source-map)
                 (update-build-output-dir)))
             all-builds))))

(defn- update-figwheel-options [options]
  (let [target-path (:target-path (task-options))]
    (-> options
      (update-in [:figwheel-options :http-server-root]
        #(or % target-path)))))

(deftask figwheel "Figwheel interface for Boot repl"
  [b build-ids        BUILD_IDS [str] "Figwheel build-ids"
   c all-builds       ALL_BUILDS edn  "Figwheel all-builds compiler-options"
   o figwheel-options FW_OPTS    edn  "Figwheel options"
   t target-path      PATH       str  "(optional) target-path specifier"]
  (assert-deps)
  (util/info "Require figwheel-sidecar.system just-in-time...\n")
  (require
   '[figwheel-sidecar.system :as fs]
   '[com.stuartsierra.component :as component])
  (boot/task-options! figwheel (fn [opts] (merge opts *opts*)))
  identity)

(definline ^:private task-options [] '(:task-options (meta #'figwheel)))

(defn make-start-fw-task-options []
  (boot/task-options!
   figwheel
   (fn [{:keys [all-builds] :as opts}]
     (-> opts
       (update :target-path #(or % "target"))
       (update :build-ids   #(or % (mapv :id all-builds))))))
  (-> (task-options)
    (select-keys [:build-ids :all-builds :figwheel-options])
    (update-output-path)
    (update-figwheel-options)))

(def ^:dynamic *boot-figwheel-system* nil)

(defn- -start-figwheel! []
  ((r fs/start-figwheel!) (make-start-fw-task-options)))

(defn start-figwheel!
  "If you aren't connected to an env where fighweel is running already,
  this method will start the figwheel server with the passed in build info."
  []
  (when *boot-figwheel-system*
    (alter-var-root #'*boot-figwheel-system* (r component/stop)))
  (alter-var-root #'*boot-figwheel-system*
    (fn [_] (-start-figwheel!))))

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

(deftask boot-figwheel "Start figwheel system"
  [i ids IDS [str]]
  (boot/task-options! figwheel #(assoc % :build-ids ids))
  (start-figwheel!)
  identity)

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

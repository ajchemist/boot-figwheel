(ns boot-figwheel.test
  (:require
   [clojure.test :refer [deftest testing is]]
   [clojure.string :as string]
   [clojure.java.io :as jio]
   [clojure.java.shell :as shell]
   [boot.core :as boot]
   [boot-figwheel :refer :all]))

(def pwd (System/getProperty "user.dir"))

(defn exec [& cmd]
  (testing cmd
    (when-not (identical? pwd shell/*sh-dir*)
      (print "On" (str "\"" shell/*sh-dir* "\", ")))
    (println "Running" (str "\"" (string/join " " cmd) "\""))
    (let [{:keys [exit out err dir]} (apply shell/sh cmd)]
      (is (= exit 0))
      (when-not (string/blank? err)
        (binding [*out* *err*]
          (println err)))
      (when-not (string/blank? out)
        (println out)))))

(deftest make-proper-task-options
  (boot/task-options! figwheel {})
  (make-start-fw-task-options)
  (is (= (get-in (meta #'figwheel) [:task-options :target-path]) "target"))

  ;; alt-target
  (boot/task-options! figwheel {:target-path "alt-target"})
  (let [{{:keys [http-server-root]} :figwheel-options} (make-start-fw-task-options)]
    (is (= http-server-root "alt-target")))
  ;; resetting
  (boot/task-options! figwheel {})

  ;; update-build-output-path
  (boot/task-options! figwheel {:build-ids ["main"] :all-builds [{:id "main"}]})
  (let [{[main-build] :all-builds} (make-start-fw-task-options)
        {{:keys [output-to output-dir asset-path]} :compiler} main-build]
    (is (= output-to "target/main.js"))
    (is (= output-dir "target/main.out"))
    (is (= asset-path "main.out")))
  (boot/task-options! figwheel {:build-ids ["main"] :all-builds [{:id "main" :compiler {:output-to "app.js"}}]})
  (let [{[app-build] :all-builds} (make-start-fw-task-options)
        {{:keys [output-to output-dir asset-path]} :compiler} app-build]
    (is (= output-to "target/app.js"))
    (is (= output-dir "target/main.out"))
    (is (= asset-path "main.out")))
  (boot/task-options! figwheel {:build-ids ["main"] :all-builds [{:id "main" :compiler {:output-to "subdir/app.js"}}]})
  (let [{[app-build] :all-builds} (make-start-fw-task-options)
        {{:keys [output-to output-dir asset-path]} :compiler} app-build]
    (is (= output-to "target/subdir/app.js"))
    (is (= output-dir "target/subdir/main.out"))
    (is (= asset-path "subdir/main.out")))
  (boot/task-options! figwheel {:build-ids ["main"] :all-builds [{:id "main" :compiler {:output-to "subdir/app.js" :output-dir ""}}]})
  (let [{[app-build] :all-builds} (make-start-fw-task-options)
        {{:keys [output-to output-dir asset-path]} :compiler} app-build]
    (is (= output-to "target/subdir/app.js"))
    (is (= output-dir "target/subdir"))
    (is (= asset-path "subdir")))
  (boot/task-options!
   figwheel
   {:build-ids  ["main"]
    :all-builds [{:id "main"
                  :compiler {:output-to "subdir/app.js" :output-dir ""}}
                 {:id "main-release"
                  :compiler {:optimizations :advanced
                             :output-to "subdir/app.js"}}]})
  (let [{[_ release-build] :all-builds} (make-start-fw-task-options)
        {{:keys [output-to output-dir asset-path]} :compiler} release-build]
    (is (= output-to "target/subdir/app.js"))
    (is (and (nil? output-dir) (nil? asset-path)))))

(deftest cljs-devtools-example
  (shell/with-sh-dir "example/cljs-devtools-sample"
    (exec "boot" "demo-figwheel")))

(ns boot-figwheel.test
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.java.io :as jio]
   [boot.core :as boot]
   [boot-figwheel :refer :all]))

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
    (is (= asset-path "subdir"))))

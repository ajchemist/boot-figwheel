(set-env!
 :resource-paths #{"src"}
 :dependencies
 '[[adzerk/bootlaces "0.1.13" :scope "test"]
   [adzerk/boot-test "1.1.2" :scope "test"]])

(require
 '[adzerk.bootlaces :refer :all]
 '[adzerk.boot-test :refer [test]])

(def +version+ "0.5.4-6")

(task-options!
 pom {:project 'ajchemist/boot-figwheel
      :version +version+
      :description "Boot task providing a Figwheel for ClojureScript development."
      :url "https://github.com/aJchemist/boot-figwheel"
      :scm {:url "https://github.com/aJchemist/boot-figwheel"}
      :license {"Eclipse Public License - v 1.0" "http://www.eclipse.org/legal/epl-v10.html"}}
 aot {:all true}
 jar {:main 'boot-figwheel}
 test {:namespaces #{'boot-figwheel.test}}
 push {:repo "deploy-clojars"})

(deftask test-profile []
  (merge-env!
   :source-paths #{"test"}
   :dependencies
   '[[org.clojure/clojure "1.8.0" :scope "provided"]
     [boot/core "2.6.0" :scope "test"]
     [org.clojure/tools.nrepl "0.2.12" :scope "test"]
     [com.cemerick/piggieback "0.2.1" :scope "test"]
     [figwheel-sidecar "0.5.4-7" :scope "test"]
     [ring/ring-core "1.5.0"
      :scope "test"
      :exclusions
      [org.clojure/tools.reader
       org.clojure/clojure]]])
  identity)

(deftask build
  "Build project.

  boot -P build push-release"
  []
  (comp #_(aot) (pom) (jar) (install))) ; (build-jar)

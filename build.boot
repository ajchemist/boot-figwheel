(set-env!
 :resource-paths #{"src"}
 :dependencies   '[[org.clojure/clojure "1.7.0" :scope "provided"]
                   
                   [boot/core "2.5.5" :scope "test"]
                   [adzerk/bootlaces "0.1.13" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.5.2-0")

(task-options!
 pom {:project 'ajchemist/boot-figwheel
      :version +version+
      :description "Boot task providing a Figwheel for ClojureScript development."
      :url "https://github.com/aJchemist/boot-figwheel"
      :scm {:url "https://github.com/aJchemist/boot-figwheel"}
      :license {"Eclipse Public License - v 1.0" "http://www.eclipse.org/legal/epl-v10.html"}}
 aot {:all true}
 jar {:main 'boot-figwheel}
 push {:repo "deploy-clojars"})

(deftask build "Build project." [] (comp (aot) (pom) (jar) (install))) ; (build-jar)

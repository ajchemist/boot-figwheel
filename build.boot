(set-env!
 :source-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.7.0" :scope "provided"]
                 [boot/core "2.1.2"]

                 [adzerk/bootlaces   "0.1.12" :scope "test"]
                 [environ "1.0.0" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.3.7-1")

(task-options!
 pom {:project 'ajchemist/boot-figwheel
      :version +version+
      :description "Boot task providing a Figwheel for ClojureScript development."
      :url "https://github.com/aJchemist/boot-figwheel"
      :scm {:url "https://github.com/aJchemist/boot-figwheel"}
      :license {"Eclipse Public License - v 1.0" "http://www.eclipse.org/legal/epl-v10.html"}}
 aot {:all true}
 jar {:main 'boot-figwheel})

(deftask build "Build project." [] (comp (pom) (aot) (jar) (install)))

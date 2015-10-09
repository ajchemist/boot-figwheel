# boot-figwheel

####Current version:
[![Clojars Project](https://clojars.org/ajchemist/boot-figwheel/latest-version.svg)](http://clojars.org/ajchemist/boot-figwheel)

#### Usage
[](dependency)
```clojure
[ajchemist/boot-figwheel "0.4.1-0"] ;; latest release
```
[](/dependency)

You don't need to add `figwheel`,`figwheel-sidecar` or of course `lein-figwheel` to dependency.

[](require)
```clojure
(require '[boot-figwheel :refer :all])
```
`figwheel`<br/>
`run-figwheel`<br/>
`destroy-figwheel`<br/>
`stop-figwheel`<br/>
`start-figwheel`

[](/require)

```clojure
(task-options!
 figwheel {:figwheel-config
           (let [p (rand-port)]
             {:builds [{:id "dev"
                        :source-paths ["src"] ; dummy
                        :compiler (merge none-opts
                                         {:main "adzerk.boot-cljs-repl"
                                          :output-to "target/app.js"
                                          :output-dir "target/out"
                                          :asset-path "out"})
                        :figwheel {:websocket-url (format "ws://localhost:%d/figwheel-ws" p)
                                   :build-id "dev"
                                   :on-jsload "<<ns.core>>.main"
                                   :heads-up-display true
                                   :autoload true
                                   :debug false}}]
              :figwheel-server {:repl true
                                :server-port p
                                :http-server-root "target"
                                :css-dirs ["target"]
                                :open-file-command "emacsclient"}})})
```

```clojure
(deftask dev []
  (set-env! :source-paths #(into % ["src"]))
  (comp (figwheel) (cljs-repl) (wait)))
```

When dev repl has been fired,

```clojure
boot.user> (run-figwheel)     ; start figwheel server in a new pod and fire autobuild
boot.user> (stop-figwheel)    ; stop autobuild
boot.user> (start-figwheel)   ; restart autobuild
boot.user> ...                ; over and over and over again

boot.user> (start-repl)       ; maybe want to fire cljs-repl (`weasel' repl wrapped by `boot-cljs-repl')
cljs.core> :cljs/quit
boot.user> (stop-figwheel)

boot.user> (destroy-figwheel)
boot.user> (run-figwheel)
boot.user> ...
```

Boot `:source-paths` env get passthru internal state of `figwheel` task at `run-figwheel` pod generation time. So it can work with `cljs-repl`.

#### Limitation

Figwheel has own fileset watcher. It can't be cooperated with boot-clj `watch`
task. So you have to edit a file in `target-path` directly, if you want to use
features like figwheel css live reloading, etc.

Some day filtered(?) watch loop may be emerged.

Personally I recommend [garden](https://github.com/noprompt/garden) to spit a
compiled css file in dev runtime.

# boot-figwheel

[Fiwheel] interface for [Boot] repl.

boot-figwheel currently intend to provide same api that exist in [figwheel-sidecar.repl-api](https://github.com/bhauman/lein-figwheel/blob/7f3cd40d6beb24ad5914222b6231fa2f98f1de03/sidecar/src/figwheel_sidecar/repl_api.clj).

## Current version:
[](dependency)
```clojure
[ajchemist/boot-figwheel "0.5.0-1"] ;; latest release
[com.cemerick/piggieback "0.2.1" :scope "test"]
[figwheel-sidecar "0.5.0-2" :scope "test"]
```
[](/dependency)

**NOTE**: Version 0.5.0 changed how the REPL dependencies are handled. For now user is required to add dependencies to one's project which are necessary libraries. `figwheel` task will print the required dependecies when run. This change related to [this](https://github.com/adzerk-oss/boot-cljs-repl/commit/e05d587240a46067633362f8aa0164ea8ed61f52).

## Usage

[](require)
```clojure
(require 'boot-figwheel)
(refer 'boot-figwheel :rename '{cljs-repl fw-cljs-repl}) ; avoid some symbols
```
[](/require)

```clojure
(task-options!
 figwheel {:build-ids  ["dev"]
           :all-builds [{:id "dev"
                         :compiler {:main 'app.core
                                    :output-to "app.js"}
                         :figwheel {:build-id  "dev"
                                    :on-jsload 'app.core/main
                                    :heads-up-display true
                                    :autoload true
                                    :debug false}}]
           :figwheel-options {:repl true
                              :http-server-root "target"
                              :css-dirs ["target"]
                              :open-file-command "emacsclient"}})
```

```clojure
(deftask dev []
  (set-env! :source-paths #(into % ["src"]))
  (comp (repl) (figwheel)))
```
`figwheel` task injects `:source-paths` environment to your figwheel `all-builds` configuration. And it also checks build `:output-to` so that the build output locate in `:target-path` environment.

When dev repl has been fired, ordinary dev routines follow.
```clojure
boot.user> (start-figwheel!)
boot.user> (start-autobuild)
boot.user> (stop-autobuild)
boot.user> (fw-cljs-repl)
cljs.core> (fig-status)
cljs.core> :cljs/quit
boot.user> (stop-figwheel!)
```

## Limitation

Figwheel has own fileset watcher. It can't be cooperated with boot-clj `watch` task. So if you want to use features like figwheel css live reloading, etc; you have to edit a file in `:target-path` directly

Personally I recommend [garden](https://github.com/noprompt/garden) to spit a
compiled css file in dev runtime in repl.

## Change

### 0.5.0
- [ BREAKING ] Figwheel has changed a lot since `0.5.0` release. So boot-figwheel have had to adapt to it. Now boot-figwheel doesn't make another pod for `figwheel` and `figwheel` runs on the same pod where your app runs. But figwheel-sidecar is only required when current boot task is  compose of `figwheel` task.

## License

Copyright © 2015 aJchemist

Licensed under Eclipse Public License.

[Boot]:           https://github.com/boot-clj/boot
[boot-cljs-repl]: https://github.com/adzerk-oss/boot-cljs-repl
[Fiwheel]:        https://github.com/bhauman/lein-figwheel

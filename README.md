# boot-figwheel

####Current version:
[![Clojars Project](http://clojars.org/ajchemist/boot-figwheel/latest-version.svg)](http://clojars.org/ajchemist/boot-figwheel)

#### Usage
[](dependency)
```clojure
[ajchemist/boot-figwheel "0.3.7-0"] ;; latest release
```
[](/dependency)

[](require)
```clojure
(require '[boot-figwheel :refer [figwheel run-figwheel stop-figwheel start-figwheel]])
```
[](/require)

#### Limitation

Figwheel has own fileset watcher. It can't be cooperated with boot-clj `watch`
task. So you have to edit a file in `target-path` directly, if you want to use
features like figwheel css live reloading, etc.


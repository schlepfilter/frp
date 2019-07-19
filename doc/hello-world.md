# Hello World
## Code
`hello-world.clj`
```clojure
(ns hello-world
  (:require [frp.core :as frp]))

(frp/defe greeting)

(frp/run println greeting)

(frp/activate)

(greeting "hello world")
```

When I load this file in a REPL, I'll see "hello world" printed in the console.

## How It Works
```clojure
(ns hello-world
  (:require [frp.core :as frp]))
```
I require the `frp.core` namespace.

```clojure
(frp/defe greeting)
```
I define an event named `greeting`. `frp/defe` stands for "define events."

```clojure
(frp/run println greeting)
```
I specify what side effect will be `run` when the `greeting` event occurs.

```clojure
(frp/activate)
```
I activate the `greeting` event. Without this, the side effect will not be run.

```clojure
(greeting "hello world")
```
I pass "hello world" to the `greeting` event. When I do this, the side effect associated with the `greeting` event will be run.

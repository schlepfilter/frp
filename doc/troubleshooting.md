# Troubleshooting
## Nothing Happens
I check if I called `frp/activate`.

## Memory Leaks
### goog.DEBUG
By default, events retain all their occurrences for the convenience of debugging.
`:closure-defines {goog.DEBUG false}` ensures that some occurrences are discarded to free memory.

### Other Causes
If I define an event or behavior that retains all its occurrences, its memory usage monotonically increases.

```clojure
(require '[frp.clojure.core :as core])
(require '[frp.core :as frp])

(frp/defe e0)

(def e1
  (core/vector e0))

(frp/activate)

(e0 0)

(e0 1)

@e1
;=> (#[tuple #[time 0] []] #[tuple #[time 4] [0]] #[tuple #[time 6] [0 1]])
```

This kind of increase in memory usage is not specific to frp. If I keep adding values to a mutable collection, its memory usage monotonically increases.

```clojure
(def a
  (atom []))

(swap! a (partial cons 0))

(swap! a (partial cons 1))
```

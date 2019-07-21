# Best Practices
## frp/defe
When defining events without occurrences, I prefer `frp/defe` to `frp/event` because `frp/defe` is terser.

```clojure
(require '[frp.core :as frp])

(frp/defe e)

(def e
  (frp/event))
```

## @ (deref)
I use `@` only for debugging because `@` is not part of the denotational semantics.

## goog.DEBUG
I add `:closure-defines {goog.DEBUG false}` to ClojureScript compiler settings for production builds.
By default, in ClojureScript, events retain all their occurrences for the convenience of debugging.
`:closure-defines {goog.DEBUG false}` ensures that some occurrences are discarded to free memory.

# Best Practices
## frp.core/defe
When defining events without occurrences, I prefer `defe` to `frp.core/event` because `defe` is terser.

```clojure
(require '[frp.core :as frp])

(frp/defe e)

(def e
  (frp/event))
```

## cats.core/=<< (reverse bind)
I prefer `=<<` to `cats.core/>>=` (bind) because the last argument of `=<<` is an event or a behavior like other functions. A consistent argument order often allows me to use `->>` in nested forms.

## @ (deref)
I use `@` only for debugging because `@` is not part of the denotational semantics.

## goog.DEBUG
I add `:closure-defines {goog.DEBUG false}` to ClojureScript compiler settings for production builds.
By default, in ClojureScript, events retain all their occurrences for the convenience of debugging.
`:closure-defines {goog.DEBUG false}` ensures that some occurrences are discarded to free memory without violating the denotational semantics.

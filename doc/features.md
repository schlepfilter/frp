# Features
frp is a functional reactive programming library. It has denotational semantics, provides the tools to avoid mutable state and provides events and behaviors as first-class objects. frp separates the pure from the impure, in that side effects are presumed to be isolated. The philosophy behind frp is that most parts of most reactive programs should be functional, and that programs that are more functional are more robust.

## Denotational semantics
frp has denotational semantics for its primitives. Useful functions can be derived by combining those primitives. Precisely defined primitives and derived functions yield insight into the essence of a type and allow clear reasoning when using them.

## Glitch avoidance
Glitches produce incorrect state and wasteful recomputation. The denotational semantics guarantee that glitches won't happen.

## First-class events and behaviors
`event` creates an event; `behavior` creates a behavior. They yield a value like any other -- you can store them in vars, pass them to functions etc.

```clojure
(require '[frp.core :as frp])

(def e 
  (frp/event))

(def b
  (frp/behavior 0))
```

## Immutability
Events and behaviors are immutable. Future occurrences of events and return values of behaviors may not be known, but that doesn't change the fact that events and behaviors are immutable. Since they can't be changed, 'adding' or 'removing' something from an immutable events or 'modifying' behaviors means creating a new event or behavior just like the old one but with the needed change.

## Separation of the pure and the impure
Most functions that take a function as the first argument assume that the argument is a pure function. An exception is `run` which is meant to be used for side effects.

# frp.core
## frp.core/event
`event` creates an event.
```clojure
(require '[frp.core :as frp])

(frp/event)
;=> #[event :0 :0]
```
The first keyword is the net id. The last keyword is the event id. I use these ids only for debugging.

If I call `event` with arguments, the event has occurrences at time 0.
```clojure
(def e
  (frp/event 0 1))
  
@e
;=> (#[tuple #[time 0] 0] #[tuple #[time 0] 1])
```
I use `@` (deref) to see the occurrences only for debugging.

The return value of a sequence of occurrences. The first value of an occurrence is the time of the occurrence. The second value is the value of the occurrence.

If I call `event` with no argument, the event doesn't have any occurrence.
```clojure
(def e
  (frp/event))

@e
;=> ()
```
I don't usually call `event` with no argument. Instead, I use `frp.core/defe`.

If I call an event with a value, the value becomes an occurrence at the time of calling the event.
```clojure
(def e
  (frp/event 0))

@e
;=> (#[tuple #[time 0] 0])
```

`event` with no argument is `mempty` in the denotational semantics.

`event` with one argument is `pure` in the denotational semantics.

## frp.core/defe
`defe` creates events with no occurrences and binds the events to the argument symbols.

```clojure
(require '[frp.core :as frp])

(frp/defe e0 e1)

@e0
;=> ()

@e1
;=> ()
```

## frp.core/behavior
I don't usually use `behavior`.

`behavior` creates a behavior with a constant.
```clojure
(require '[frp.core :as frp])

(def b
  (frp/behavior 0))

@b
;=> 0
```
I use `@` (deref) to see the current value only for debugging.

`behavior` is `pure` in the denotational semantics.

## frp.core/time
`time` is a behavior. Its current value is milliseconds since `activate` is called.
```clojure
(require '[frp.core :as frp])

@frp/time
;=> #[time 0]

(frp/activate 100)

@frp/time
;=> #[time 3]

@frp/time
;=> #[time 1010]
```

If I use `time`, I call `frp.core/activate` with a rate of sampling in milliseconds. 

## frp.core/stepper
`stepper` converts an event to behavior with a default value.
```clojure
(require '[frp.core :as frp])

(frp/defe e)

(def b
  (frp/stepper 0 e))

@b
;=> 0

(frp/activate)

(e 1)

@b
;=> 1
```

## frp.core/transduce
I don't usually use `transduce`. Instead, I use `frp.clojure.core/reduce` and other functions.

`transduce` transforms an event with a transducer and a reducing function.
```clojure
(frp/defe e0)

(def e1
  (frp/transduce (map inc) + e0))

(frp/activate)

(e0 0)

(e0 1)

@e1
;=> (#[tuple #[time 4] 1] #[tuple #[time 12] 3])
```

## frp.core/snapshot
`snapshot` captures the return values of behaviors at each time of occurrence of an event and puts the occurrence and the return values in a sequence.
```clojure
(require '[frp.core :as frp])

(def e0
  (frp/event 0))

(def b
  (frp/behavior 1))

(def e1
  (frp/snapshot e0 frp/time b))

(frp/activate 100)

@e1
;=> (#[tuple #[time 0] (0 #[time 0] 1)])

(e0 2)

@e1
;=> (#[tuple #[time 0] (0 #[time 0] 1)] #[tuple #[time 1368] (2 #[time 1368] 1)])
```

## frp.core/activate
`activate` starts time and enables events to receive occurrences.
```clojure
(require '[frp.core :as frp])

(frp/defe e)

(e 0)

@e
;=> ()

(frp/activate)

(e 1)

@e
;=> (#[tuple #[time 4] 0] #[tuple #[time 6] 1])
```
If I send occurrences to an event before `frp.core/activate` is called, the occurrences get queued. These occurrences get processed after `frp.core/activate` is called.

## frp.core/run
`run` attaches a side effect to an event or a behavior.

Code:
```clojure
(require '[frp.core :as frp])

(frp/defe e)

(frp/run println e)

(frp/activate)

(e 0)

(e 0)
```
Console:
```
0
0
```
The side effect attached to an event gets called for each occurrence of the event even if the occurrence's value doesn't change.

Code:
```clojure
(frp/defe e)

(def b
  (frp/stepper 0 e))

(frp/run println b)

(frp/activate)

(e 0)

(e 0)

(e 1)
```

Console:
```
0
1
```
The side effect attached to a behavior gets called only when the return value of the behavior changes.

## frp.core/transparent
`transparent` traverses the form recursively and turns arguments into events or behaviors and lifts functions.
```clojure
(require '[frp.core :as frp])

(def b
  (frp/transparent (+ (frp/behavior 1) 2)))

@b
;=> 3
```

## frp.core/undoable
`undoable` creates an undoable event or behavior.

The last argument is the definition of the undoable event or behavior.

The second to last argument is a sequence of events to undo for the undoable event.
```clojure
(require '[frp.core :as frp])

(frp/defe undo redo action)

(def b
  (frp/undoable 10 undo redo [action] (frp/stepper 0 action)))

(frp/activate)

(action 1)

@b
;=> 1

(undo)

@b
;=> 0

(redo)

@b
;=> 1
```

## frp.core/accum
`accum` takes an initial value and an event of functions as arguments and returns an event. The returned event's first occurrence is the return value of the first function applied to the initial value. The next occurrence is the return value of the next function applied to the previous occurrence.

```clojure
(frp/defe e0)

(def e1
  (frp/accum 0 e0))

(frp/activate)

(e0 inc)

@e1
;=> (#[tuple #[time 0] 0] #[tuple #[time 4] 1])

(e0 dec)

@e1
;=> (#[tuple #[time 0] 0] #[tuple #[time 4] 1] #[tuple #[time 1397] 0])
```

## frp.core/switcher
I don't usually use `switcher`. Instead, I use `frp.core/stepper`.

`switcher` takes a default behavior and an event of behaviors and returns a behavior. The returned behavior is the latest occurrence of the event if such an occurrence exists. Otherwise, the returned behavior defaults to the default behavior.

```clojure
(require '[frp.core :as frp])

(frp/defe e)

(def b
  (frp/switcher (frp/behavior 0) e))

@b
;=> 0

(frp/activate)

(e (frp/behavior 1))

@b
;=> 1
```

## frp.core/restart
`restart` resets the state of frp.

I use `restart` for debugging in Clojure. In ClojureScript, I don't use `restart` because Figwheel resets the state of frp with automatic reloading.

# cats.core
## cats.core/<> (mappend)
`<>` combines two or more events into one event.
```clojure
(require '[cats.core :as m])
(require '[frp.core :as frp])

(def e
  (m/<> (frp/event 0) (frp/event 1)))

@e
;=> (#[tuple #[time 0] 0] #[tuple #[time 0] 1])
```

## cats.core/<$> (fmap)
`<$>` maps the function on an event or a behavior.
```clojure
(require '[cats.core :as m])
(require '[frp.core :as frp])

(def e
  (m/<$> inc (frp/event 0)))

@e
;=> (#[tuple #[time 0] 1])
```

```clojure
(def b
  (m/<$> inc (frp/behavior 0)))

@b
;=> 1
```

## cats.core/=<< (reverse bind)
`=<<` maps a function on an event and combines the returned events into one event.
```clojure
(require '[cats.core :as m])
(require '[frp.core :as frp])

(def inner
  (frp/event 0))

(def outer
  (m/=<< frp/event inner))

(frp/activate)

@outer
;=> (#[tuple #[time 0] 0])

(inner 1)

@outer
;=> (#[tuple #[time 0] 0] #[tuple #[time 1618] 1])
```

`=<<` maps a function on a behavior and combines the returned behaviors into one behavior.
```clojure
(require '[cats.core :as m])
(require '[frp.core :as frp])

(def e
  (frp/event 0))

(def b0
  (frp/stepper 1 e))

(def b1
  (m/=<< frp/behavior b0))

(frp/activate)

@b1
;=> 0

(e 2)

@b1
;=> 2
```

# aid.core
The functions in `aid.core` may become deprecated if these functions are implemented or patched in other libraries.

## aid.core/<$
`<$` maps the occurrences of an event or the values of an behavior to a constant value.
```clojure
(require '[aid.core :as aid])
(require '[frp.core :as frp])

(def e
  (aid/<$ 1 (frp/event 0)))

@e
;=> (#[tuple #[time 0] 1])
```

```clojure
(def b
  (aid/<$ 1 (frp/behavior 0)))

@b
;=> 1
```

## aid.core/lift-a
`lift-a` lifts a function. A lifted function take behaviors as arguments and returns a behavior.
```clojure
(require '[aid.core :as aid])
(require '[frp.core :as frp])

(def b
  ((aid/lift-a +) (frp/behavior 1) (frp/behavior 2)))

@b
;=> 3
```
"a" in `lift-a` stands for applicative.

# frp.clojure.core
A function in `frp.clojure.core` is similar to that of the same name in `clojure.core`.

All the parameters but the last of a function in `frp.clojure.core` are the same as those of a corresponding function in `clojure.core`.

The last parameter of a function in `frp.clojure.core` is an event.
 
The corresponding parameter in `clojure.core` can be a collection. `reduce` is one such function.
```clojure
(require '[frp.clojure.core :as core])
(require '[frp.core :as frp])

(frp/defe e0)

(def e1
  (core/reduce + e0))

(frp/activate)

(e0 1)

(e0 2)

@e1
;=> (#[tuple #[time 7] 1] #[tuple #[time 20] 3])
```

The corresponding parameters in `clojure.core` can be multiple parameters. `+` is one such function.
```clojure
(require '[frp.clojure.core :as core])
(require '[frp.core :as frp])

(frp/defe e0)

(def e1
  (core/+ e0))

(frp/activate)

(e0 1)

(e0 2)

@e1
;=> (#[tuple #[time 5] 1] #[tuple #[time 9] 3])
```

# frp.incanter.distributions
A function in `frp.clojure.core` is similar to that of the same name in `incanter.distributions`.

# frp.ajax
A function in `frp.ajax` is similar to that of the same name in `cljs-ajax.core`. The occurrence of the returned event is the response of the request.

```clojure
(def e
  (GET "https://api.github.com" {}))
```

# frp.document
## event
* frp.document/visibilitychange

## behaviors
* frp.document/hidden
* frp.document/visibility-state

# frp.history
## event
* frp.history/pushstate

## frp.history/push-state
`push-state` is similar to `js/history.pushState`. `push-state` sends an occurrence to `frp.history/pushstate`.
```clojure
(history/push-state {} "" "/path")
```

# frp.window
## events
* frp.window/blur
* frp.window/click
* frp.window/contextmenu
* frp.window/copy
* frp.window/cut
* frp.window/dragend
* frp.window/dragleave
* frp.window/dragstart
* frp.window/drop
* frp.window/focus
* frp.window/input
* frp.window/keydown
* frp.window/keypress
* frp.window/keyup
* frp.window/paste
* frp.window/pointerdown
* frp.window/pointermove
* frp.window/pointerout
* frp.window/pointerover
* frp.window/pointerup
* frp.window/popstate
* frp.window/resize
* frp.window/scroll
* frp.window/submit
* frp.window/whee

## behaviors
* frp.window/inner-height
* frp.window/inner-weight
* frp.window/outer-height
* frp.window/outer-width
* frp.window/scroll-x
* frp.window/scroll-y
# DOM Events
There are two ways to integrate DOM events.

The general way is to define an FRP event and specify the FRP event in the event handler of a DOM element.

```clojure
(frp/defe click)

(def c
  [:button {:on-click #(click)}])
```

Another way is to use predefined FRP events in `frp.window` and other namespaces.

```clojure
window/click
```

One difference between the two examples above is that `window/click` captures `click` DOM events from different DOM elements in the window whereas `:on-click` captures `click` DOM events only in the `button`.
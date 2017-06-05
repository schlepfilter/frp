# Rationale

## Why?

Why did I write yet another reactive programming library?  Basically because I wanted:
* A Lisp flavored Embedded Domain-specific Language
* packaged via Standard Type Classes
* with (Pseudo) Transparent Reactivity
* for Functional Reactive Programming (FRP)

and couldn't find one.  Here's an outline of some of the motivating ideas.

## Lisp is a good thing
* Often emulated/pillaged, still not duplicated
* Lambda calculus yields an extremely small core
* Almost no syntax
* Core advantage still code-as-data and syntactic abstraction

## Standard type classes are good things
* Type class polymorphic functions
* Behavior obeys type class morphism

## (Pseudo) transparent reactivity is a good thing
* Event functions emulating Seq functions
* Implicit lifting for behaviors
* Implicit combining for events
* Native events packaged via Events and Behaviors

## FRP is a good thing
* Denotational semantics + continuous time
* Could be done in non-reactive libraries by discipline/convention
  * But if a data structure can be mutated, dangerous to presume it won't be
* Could be done in Non-FRP libraries by discipline/convention
  * But avoiding glitch by hand leads to convoluted code
* Could be done without continuous time
  * But emulating Behavior with Event complects them
* FRP libraries tend to be statically typed
  * Not for everyone, or every task

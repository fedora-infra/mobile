<!-- vim:syntax=markdown
-->

We're thrilled that you're considering contributing to the Fedora Mobile
Android app.

Here's a few short notes about how we do things around here.

We make heavy use of Scala and the scalaz and argonaut libraries which make it
easier to reason about our code and perform refactorings such as
[this one](https://github.com/fedora-infra/mobile/commit/36965b98fc167ed44a0c7ef6becb4a647305b78c)
with a reasonably high chance of things not breaking once you get the code to
start compiling again.

The Android APIs make it *extremely* difficult to write purely functional
applications. Such an effort would require wrapping a *lot* of classes with
scalaz's `Free` monad.

That said, we try to preserve referential transparency as much as possible and
when opportunities arise to restructure code to be referentially transparent
(and there are quite a lot of areas as of this writing that can be refactored),
we try to take advantage of them.

For side-effecty code that must fork to its own thread (e.g. to perform network
activity), make heavy use of `Task`. Also, try to avoid anti-patterns such as
pattern-matching on algebraic datatype constructors, when a `fold` (or
"catamorphism") will work just as well. (There are some cases where this is
less than possible because you'll lose type inference or tail call
elimination). Pattern matching doesn't compose, `fold` does.

Above all, be reasonable. Use 
[parametricity](https://dl.dropboxusercontent.com/u/7810909/media/doc/parametricity.pdf) [1].
A lot. Use equational reasoning to compositionally build up larger functions.
Use abstractions. This sometimes means using big scary words like "monad" and
"functor" and "applicative functor." But they are friendly. Really. Use them.
**Befriend them.**

[1] http://citeseer.ist.psu.edu/viewdoc/summary?doi=10.1.1.38.9875

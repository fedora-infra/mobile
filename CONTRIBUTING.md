# Contributing to Fedora Mobile

(See README.md for setting up the environment).

Welcome! We're glad that you would like to contribute to Fedora Mobile, Fedora's
Android application that allows our community to interact with our
infrastructure from anywhere.

We primarily use Scala to develop the Android application, which leads to less
code/verbosity, and more productivity.

That said, Scala and Java can interact with each other very nicely, and we will
gladly accept patches in either language.

Fedora Mobile is licensed under the Mozilla Public License, version 2.0. See
this
[tldrlegal](http://www.tldrlegal.com/license/mozilla-public-license-2.0-%28mpl-2%29])
page for a quick summary of what that means.

# Code Guidelines/Notes - Scala

We have a few guidelines in place for ensuring readability throughout the
codebase. First, take a look at this
[style guide](http://docs.scala-lang.org/style/). It's the one we adhere to in
most places.

We use the Scalariform tool to ensure consistency in formatting. To do a
Scalariform pass, open up SBT in the project, and run the `scalariform-format`
task.

There's a few things that Scalariform can't enforce, however. For example, **try
as best you can to limit your lines to 80 characters**, although **100 is
tolerable**. Beyond 100 is iffy, try to avoid it.

---

Unless there's a good reason not to, all files that you add should be in the
`org.fedoraproject.mobile` package. To do this, add

```scala
package org.fedoraproject.mobile
```

to the top line of the file.

---

Consistent import ordering helps developers know where to look to find where a
specific import might be, at the top of a file.

Our ordering should work like this, with each group separated by a blank line:

- Our local imports (e.g., `Implicits._`)
- Android APIs
- Third-party APIs (each designator (e.g., `com.` and `net.`) separated by a
  blank line)
- Scala APIs
- Java APIs

---

We define a small number of "implicit" methods that are handy in many cases. To
take advantage of these, add `import Implicits._` to your file.

One such example is that we can implicitly convert a function to a Runnable.

So instead of this Java code:

```java
runOnUiThread(new Runnable() {
  @Override
  public void run() {
    btn.setText("submit");
  }
});
```

...you can just do `runOnUiThread { btn setText "submit" }` so long as you
imported our `Implicits` object.

As you can see, this is one example of using Scala as a means to write less code
than the Java alternative.

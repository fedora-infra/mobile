# Fedora Mobile

<img src="http://shieldkins.elrod.me/fedora/fedora-mobile" alt="Fedora Mobile Status" />

Fedora Mobile is a one-stop-shop application for interacting with the Fedora
community you know and love, on the go.

It provides access to several applications run throughout the Fedora
Infrastructure, and allows contributors and users to stay involved without
being in front of a computer.

Some things that Fedora Mobile sets out to do:

- Provide account synchronization between your phone and the
  [Fedora Accounts System](https://admin.fedoraproject.org/accounts/) using the
  Android Accounts Provider API.
- Provide access to [Fedocal](https://apps.fedoraproject.org/calendar/)
- Be able to quickly check the [status](http://status.fedoraproject.org/) of
  Fedora services.
- [Tag packages](https://apps.fedoraproject.org/tagger/)
- [Search packages](https://apps.fedoraproject.org/packages/)
- View the [Fedora Badges](https://badges.fedoraproject.org/) leaderboard.
- And more.

We target Android 3.0+ for the time being. The reason for this is that Fedora's
mission is to "lead the **advancement** of free and open source software and
content as a collaborative community" - advancement being key.

Not having to deal with 2.x's quirks also lets us do some things more cleanly
and take advantage of newer features like Navigation Drawers and the Action Bar
without having to depend on thirdparty hacks/libraries.

# Trying it out

You can get the latest working HEAD build from the Fedora Infrastructure
Jenkins instance at
http://jenkins.cloud.fedoraproject.org/job/fedora-mobile/ws/target/fedora-mobile-0.1.apk

A short URL that goes to the same thing (easier for typing on phones and
tablets) is http://da.gd/j

# Screenshots

Because the readme is too short and I need to fill some space. ;)

## Status

<img src="http://i.imgur.com/HHnvOc3.png" />

# Setting up a development environment

Fedora Mobile is primarily written in Scala - but don't let this scare you.

If you're familiar with the Android SDK, you'll find the Fedora Mobile code
really familiar. Because Scala runs on the JVM, we can use the same Java
libraries that native Java programs use, within our code. Yep, a `ListView`
is still a `ListView`.

(Ideally we would make very heavy use of scalaz's `Free` and wrap the Android
SDK like crazy, but I digress).

For the rest of this section, we'll assume that you're on a fairly modern
Fedora version -- if not, some things might differ slightly. The general
idea is to get a working SBT (Simple Build Tool) launcher working, and
to get the Android SDK somewhere safe that SBT can read.

This guide does NOT assume any particular IDE - in fact, I (@CodeBlock) solely
use Emacs to develop. However, if you must use an IDE, maybe consider the
[Scala IDE](http://www.scala-ide.org/) for Eclipse.

## Installing dependencies

The Android development platform requires some 32-bit packages to be installed.
We also need a working Java compiler and runtime.

```
sudo yum install glibc.i686 \
  glibc-devel.i686 \
  libstdc++.i686 \
  zlib-devel.i686 \
  ncurses-devel.i686 \
  libX11-devel.i686 \
  libXrender.i686 \
  libXrandr.i686 \
  java-1.7-openjdk \
  java-1.7-openjdk-devel
```

Source:
[Fedora Wiki](https://fedoraproject.org/wiki/HOWTO_Setup_Android_Development).


## Get the Android SDK

To start out, get the
[Android SDK](https://developer.android.com/sdk/index.html). You'll likely want
the one that says **Linux 64-bit**.

Unzip the SDK and put the result someplace safe. I use
`~/devel/android-sdk-linux/`. Discard the zip.

Permanently set your `$ANDROID_HOME` environment variable to wherever you
unzipped the SDK to. Do this by adding a line like the following to your
`.bashrc`:

```
export ANDROID_HOME=~/devel/android-sdk-linux/
```

Remember to `source ~/.bashrc` to make it take effect right away.

## Install the right API level(s)

NOTE: **Fedora Mobile supports Android 3.0 and up, which is API Level 11+.**

Open up the **Android SDK Manager** by running:

```
$ANDROID_HOME/tools/android
```

Once it comes up, select at least everything under API Level 11 and API Level
17, and click Install. This will take some time.

## The Emulator

Android provides a nice emulator that we can use for testing the app as we
develop.

Close out of the Android SDK Manager, and open the **Android Virtual Device
Manager**. Do this by running:

```
$ANDROID_HOME/tools/android avd
```

When it comes up, do the following (changing fields as needed):

* New
* AVD Name: `android-4.2`
* Device: (anything - I use "Nexus 4")
* Target: `Android 4.2.2 - API Level 17`
* CPU: ARM

Hit OK, then do the same for API Level 11.

Close out of the Android Virtual Device Manager.

## Getting SBT

There's an SBT RPM in my (@CodeBlock) FedoraPeople repo. Throw
[this repo file](http://repos.fedorapeople.org/repos/codeblock/sbt/sbt.repo) in
`/etc/yum.repos.d/sbt.repo` and run `sudo yum install sbt `.

## Getting the code and getting started

You're almost done!

At this point, if you haven't done so, clone this repository.

If you're a committer,

```
git clone git@github.com:fedora-infra/mobile.git; cd mobile
```

Otherwise,

```
git clone git://github.com/fedora-infra/mobile.git; cd mobile
```

Run `sbt update` and wait. The first time you run SBT (and similar build tools),
it has to "download the internet." :)

Once it completes, you can just run `sbt` which will get you an SBT console.
The advantage of using the console is not having to wait for JVM to start up,
every time you want to run an SBT command.

All of the following assume you're in the SBT shell. You can do the same things
from your shell prompt by doing `sbt <the command>`, but it's slower that way.

Run `android:emulator-start android-4.2` to start up your AVD. A window should
open with your emulator. It might take a while to boot up.

Run `android:install-emulator` to compile the application and install it into
your emulator.

In most cases, you can make your changes, then just do
`android:install-emulator` to compile and run. Rarely, you might need to clean
the project of old build artifacts, and you can do that with the `clean`
command.

To compile without installing to the emulator, use `compile`. This is
sometimes quicker just for making sure syntax is fine and everything
typechecks.

To just obtain an APK (without installing into the emulator), use
`android:package-debug` or `android:package-release`. You can install that APK
to a physical device.

## Debugging

When you encounter "The application has quit" errors, it means an uncaught
exception was thrown. You can use ADB and `logcat` to see the traceback.

Run:

```
$ANDROID_HOME/platform-tools/adb shell
```

When you're in the shell, use `logcat`. The output will be verbose, but the
exception should be near the bottom.

# License

The application is licensed under the Mozilla Public License, version 2.
See LICENSE.

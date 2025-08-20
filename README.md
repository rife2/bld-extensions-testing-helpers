[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/2.3.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/releases/com/uwyn/rife2/bld-extensions-testing-helpers/maven-metadata.xml?color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-extensions-testing-helpers)
[![Snapshot](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/snapshots/com/uwyn/rife2/bld-extensions-testing-helpers/maven-metadata.xml?label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-extensions-testing-helpers)
[![GitHub CI](https://github.com/rife2/bld-extensions-testing-helpers/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-extensions-testing-helpers/actions/workflows/bld.yml)

# Testing Helpers for [b<span style="color:orange">l</span>d Extensions](https://github.com/rife2/bld/wiki/Extensions)

This project provides a collection of testing helpers used by various
[bld extensions](https://github.com/rife2/bld/wiki/Extensions).

To use, include the following in your `bld` build file:

```java
repositories = List.of(RIFE2_RELEASES, RIFE2_SNAPSHOTS);

scope(compile).include(
    dependency("com.uwyn.rife2", "bld-extensions-testing-helpers", version(0, 9,0,"SNAPSHOT"))
);
```
Please check the [documentation](https://rife2.github.io/bld-extensions-testing-helpers)
for more information.

## JUnit Annotations

The following annotations are provided:

| Annotation                                                                                                            | Description                                          |
|:----------------------------------------------------------------------------------------------------------------------|:-----------------------------------------------------|
| [`DisabledOnCi`](https://rife2.github.io/bld-extensions-testing-helpers/rife/bld/extension/testing/DisabledOnCi.html) | Disables a test when running on a CI/CD environment  |
| [`RandomRange`](https://rife2.github.io/bld-extensions-testing-helpers/rife/bld/extension/testing/RandomRange.html)   | Generates a random integer within a specified range. |
| [`RandomString`](https://rife2.github.io/bld-extensions-testing-helpers/rife/bld/extension/testing/RandomString.html) | Generates a random string.                           |


## JUnit Extensions

The following extensions are provided:

| Extension                                                                                                                     | Description                                 |
|:------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------|
| [`LoggingExtension`](https://rife2.github.io/bld-extensions-testing-helpers/rife/bld/extension/testing/LoggingExtension.html) | Configures console logging for test suites. |

## Helpers

The following helper classes are provided:

| Helper                                                                                                                    | Description                              |
|:--------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------|
| [`TestLogHandler`](https://rife2.github.io/bld-extensions-testing-helpers/rife/bld/extension/testing/TestLogHandler.html) | A log handler that captures log messages |


## Utilities

The following static methods are provided:

| Utility                                                                                                                                                         | Description                 |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------------------------|
| [`generateRandomInt(int, int)`](https://rife2.github.io/bld-extensions-testing-helpers/rife/bld/extension/testing/TestingUtils.html#generateRandomInt(int,int)) | Generates a random integer. |
| [`generateRandomString()`](https://rife2.github.io/bld-extensions-testing-helpers/rife/bld/extension/testing/TestingUtils.html#generateRandomString())          | Generates a random string.  |


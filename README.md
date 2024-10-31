# TeamCity Unreal Engine plugin

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This plugin allows you to build Unreal Engine projects in TeamCity.

## Features

* Automatically detect Unreal Engine installed on build agents
* Dedicated runner that covers the most common use cases:
    * Execute the `BuildCookRun` command to perform all stages of the build process.
    * Utilize custom BuildGraph scripts.
    * Launch automation tests.
* Automation test reporting
* UGS metadata server integration

For details on usage, please refer to [USAGE.md](USAGE.md).

## Installation

You can download the plugin from the [marketplace][marketplace.plugin-page]. Installation instructions are available
[here][plugin-installation-guide].

## How to Contribute

We place a high value on user feedback and encourage you to share your experience and suggestions. Send a Pull Request to contribute or contact us via [YouTrack][youtrack] to report an issue.

## Development

### Prerequisites

* Docker
* Amazon JDK 11

### Building

1. Clone the repo
2. Setup local git hooks
    ```shell
    git config core.hooksPath .githooks
    ```
3. Build the project using Gradle
    ```shell
    ./gradlew build
    ```

## Additional Resources

- [Usage](USAGE.md)
- [Changelog](CHANGELOG.md)
- [Maintainership](MAINTAINERSHIP.md)
- [Configuration](CONFIGURATION.md)

[youtrack]: https://youtrack.jetbrains.com/newIssue?project=TW&c=add%20Board%20TeamCity%20BTI%20%7C%20TeamCity%20Releases&c=add%20Board%20TeamCity%20Documentation%20No%20Fix%20versions&c=add%20Board%20TeamCity%20BTI&c=Team%20Build%20Tools%20Integrations&c=tag%20tc-unreal-engine
[marketplace.plugin-page]: https://plugins.jetbrains.com/plugin/22679-unreal-engine-support
[plugin-installation-guide]: https://www.jetbrains.com/help/teamcity/installing-additional-plugins.html

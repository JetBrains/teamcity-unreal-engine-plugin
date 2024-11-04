# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added
- UGS metadata server integration, implemented as an extension to the existing Commit Status Publisher feature and
within dynamically generated BuildGraph builds [TW-88798](https://youtrack.jetbrains.com/issue/TW-88798)

### Changed
- Generated BuildGraph builds (in distributed mode) now inherit the build number from the "parent" build [TW-90184](https://youtrack.jetbrains.com/issue/TW-90184/)
- Configuration parameters are now propagated to the generated BuildGraph virtual builds as well [TW-90185](https://youtrack.jetbrains.com/issue/TW-90185)

### Fixed
- Custom identifiers for Unreal Engine installations on agents are now correctly provisioned as agent parameters,
  with no duplicate dots (if present) and without curly braces (in the case of auto-generated source build IDs)

## 1.0.2 - 2024-06-14

### Changed

- Checkout settings (directory and mode) are now synced with the original build for generated BuildGraph configurations
[TW-87894](https://youtrack.jetbrains.com/issue/TW-87894)

### Fixed

- The way the project parameter is passed to the Editor when running automation tests has been changed
(The previous one caused problems on some versions of the Engine) [TW-87924](https://youtrack.jetbrains.com/issue/TW-87924)

## 1.0.1 - 2024-05-22

### Fixed

- Additional arguments for generated configurations are now inherited from the original BuildGraph configuration
in distributed mode [TW-87872](https://youtrack.jetbrains.com/issue/TW-87872)
- Build parameters for the generated "Setup" configuration are now inherited from the original BuildGraph configuration
in distributed mode [TW-87893](https://youtrack.jetbrains.com/issue/TW-87893)

## 1.0.0 - 2024-05-07

### Added

- Introduction of the distributed BuildGraph run mode
- On-the-fly test reporting [TW-83259](https://youtrack.jetbrains.com/issue/TW-83259)
- Enable support for filtering tests via RunFilter [TW-86667](https://youtrack.jetbrains.com/issue/TW-86667)
- Enable configuration parameters to be used in "Target configurations" and "Target platforms" fields [TW-86636](https://youtrack.jetbrains.com/issue/TW-86636)
- Kotlin DSL [TW-81791](https://youtrack.jetbrains.com/issue/TW-81791/Unreal-Engine-Plugin-Kotlin-DSL)

### Changed

- There are many breaking changes compared to the previous unstable 0.x.x versions.
You may need to recreate your build configurations using the new version of the plugin.

### Fixed

- Failed build not properly reported [TW-86947](https://youtrack.jetbrains.com/issue/TW-86947)

## 0.3.3 - 2024-02-26

### Fixed

- Tests are not reported if there is a failed test. [TW-86540](https://youtrack.jetbrains.com/issue/TW-86540)

## 0.3.2 - 2024-01-31

### Fixed

- Typo in the server config option when using the BuildCookRun command. [TW-86168](https://youtrack.jetbrains.com/issue/TW-86168)

## 0.3.1 - 2023-11-22

### Fixed

- Engine discovery in cases involving relative paths to the root folder in manual detection mode. [TW-85107](https://youtrack.jetbrains.com/issue/TW-85107/Unreal-Engine-plugin-runner-cant-find-the-engine-when-its-in-the-root-folder)

## 0.3.0 - 2023-11-06

### Added

- Support for UE4 [TW-84213](https://youtrack.jetbrains.com/issue/TW-84213/Unreal-Engine-Plugin-UE4-support)

### Changed

- A couple of terminology changes: "engine version" has been updated to "engine identifier."
This change may potentially affect previously configured runners due to the modification of the
corresponding parameter name. Please verify that the field is correctly filled,
or the parameter has the correct name in the Kotlin DSL

## 0.2.0 - 2023-10-18

### Added

- Allow multiple target configs specification (BuildCookRun) [TW-84015](https://youtrack.jetbrains.com/issue/TW-84015/Unreal-Engine-Plugin-allow-multiple-target-configs-specification-BuildCookRun)

### Changed

- Runner description now contains more information

### Fixed

- Escaping inner quotes in key-value additional arguments on Windows [TW-84013](https://youtrack.jetbrains.com/issue/TW-84013/Unreal-Engine-Plugin-missing-quotes-in-key-value-additional-argument-on-Windows)
- Runner description issue for auto-detected steps [TW-84231](https://youtrack.jetbrains.com/issue/TW-84231/Unreal-Engine-Plugin-issue-with-runner-description-generation)

## 0.1.1 - 2023-10-04

### Fixed

- Additional Arguments are not getting passed to Unreal's UAT Build Graph [TW-83629](https://youtrack.jetbrains.com/issue/TW-83629/Additional-Arguments-are-not-getting-passed-to-Unreals-UAT-Build-Graph)
- Cook advanced options displayed when they shouldn't be [TW-83897](https://youtrack.jetbrains.com/issue/TW-83897/Unreal-Engine-Plugin-cook-advanced-options-displayed-when-they-shouldnt-be)

## 0.1.0 - 2023-09-06

Initial release of the plugin

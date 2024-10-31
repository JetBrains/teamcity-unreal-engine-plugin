# Maintainership

## Release

To release a new version, follow these steps.

1. Choose the new version according to [Semantic Versioning][semver]. It should consist of three numbers (i.e. `1.0.0`).
2. Make sure there are some entries under `Unreleased` section in the `CHANGELOG.md`
3. Execute the following Gradle task to update the changelog
   (this task comes from the [plugin][gradle-changelog-plugin] we use to keep a changelog)
    ```shell
    ./gradlew patchChangelog -Pversion="$version"
    ```
4. Open a pull request and merge changes (you could do it beforehand in any other pr)
5. Switch to a commit you want to tag (usually it's the HEAD of the master branch) and execute
    ```shell
    ./tag-release-and-push.sh
    ```

It will tag the current `HEAD` with latest version from the changelog, and push it to the origin remote.

The new version of the plugin will be published to [marketplace][marketplace.plugin-page] automatically.

## Kotlin DSL Changes Test

To test Kotlin DSL changes, follow these steps:

1. Ensure your dev TeamCity instance is running locally (in the example below we assume it's running on http://localhost:8111)
2. Update the relevant files inside the `.kotlin-dsl` directory
3. Build the plugin and upload it to your local TeamCity instance
4. Create a test project using [Kotlin DSL][teamcity.kotlin-dsl.getting-started]
   (note, you can create a project locally and then [import][teamcity.kotlin-dsl.import-existing-project]
   it into TeamCity), and point Maven’s DSL plugins repository to your local TeamCity instance
```xml
<repositories>
    ...
    <repository>
        <id>teamcity-server</id>
        <url>http://localhost:8111/app/dsl-plugins-repository</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
    ...
</repositories>
```
5. You should now be able to use the added functionality in the test project's `.teamcity/**/*.kts` files

[semver]: https://semver.org/spec/v2.0.0.html
[marketplace.plugin-page]: https://plugins.jetbrains.com/plugin/22679-unreal-engine-support
[gradle-changelog-plugin]: https://github.com/JetBrains/gradle-changelog-plugin
[teamcity.kotlin-dsl.getting-started]: https://www.jetbrains.com/help/teamcity/kotlin-dsl.html#Getting+Started+with+Kotlin+DSL
[teamcity.kotlin-dsl.import-existing-project]: https://blog.jetbrains.com/teamcity/2019/03/configuration-as-code-part-1-getting-started-with-kotlin-dsl/#import-project-with-the-existing-kotlin-script

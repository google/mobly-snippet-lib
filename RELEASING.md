# Releasing Mobly Snippet Lib

The instructions are for developers of Mobly to release a new version of Mobly
Snippet Lib to jCenter.

### First time setup

Before cutting your very first release, do this once:

1.  Log into your bintray account
1.  Create an API key for your account
1.  In your local checkout of mobly-snippet-lib, create a file called
    `bintray.properties` in the root of the repo containing the following:

    ```
    user=<username>@google
    key=<auth key>
    ```

    This file is part of `.gitignore` so git will not prompt you to commit it.

### Cutting a new release

1.  Choose an appropriate version number

1.  Update all occurrences of the version in the codebase with the new version.
    Example: https://github.com/google/mobly-snippet-lib/pull/35

1.  Update the changelog with the highlights of what's changed. Use your
    discretion, but definitely mention any breaking API changes.

1.  Send the PR for review.

1.  Squash or rebase it into master once approved.

1.  In your local copy, rebase master onto origin/master to get the final
    release commit.

1.  Use `git tag` to tag the release commit with the corresponding version.

1.  Make doubly sure there are no uncommitted changes

1.  Push the new tag using git push --tags

1.  Compile the final artifact and upload it to bintray:

    `./gradlew clean bintrayUpload`

1.  Go to the ['mobly' project on
    bintray](https://bintray.com/google/mobly/mobly-snippet-lib).

1.  Make sure your new version appears on the website and has the right number.

1.  Click the new version number and go to the Files tab. Make sure there are 8
    files: pom, aar, sources jar, javadoc jar, and corresponding signatures
    (.asc files) for each.

1.  If everything checks out, click `Publish` on the grey bar above the files.

1.  Send a PR to bump the version for the next development iteration. Example:
    https://github.com/google/mobly-snippet-lib/pull/36

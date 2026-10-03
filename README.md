# Burton Issues

An issue tracker for Android with the same look as the other Burton apps. Sign in with **Connect with GitHub**, pick public Android apps in [Burton-Workspaces](https://github.com/Burton-Workspaces), and file, search, comment, and close issues.

Signed APKs are published on [GitHub Releases](https://github.com/Burton-Workspaces/burton-issues/releases). Droidify / F-Droid: [burton-sonos-fdroid](https://github.com/Burton-Workspaces/burton-sonos-fdroid) (`https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo`).

## What it does

- **Inbox** — open issues across the apps you subscribe to
- **Apps** — public Android repos in Burton-Workspaces; tap a row for that app’s issues
- **Issue** — body, comments, labels, assignees, milestone, close / reopen, edit
- **New issue** — pick one app from a dropdown; other Burton apps can open this screen with themselves selected
- **Search** — GitHub issue search over subscribed repos
- **Settings** — account, which apps to track, about, GitHub backend, sign out

The token stays on the phone (DataStore). The app talks to GitHub over HTTPS; there is no Burton cloud account. Other issue-tracking backends can plug in behind the same `IssueTracker` interface; GitHub is the first.

## Requirements

- Android 8.0+ (API 26)
- Internet
- A GitHub account you can authorize (see [Using the app](docs/using.md))

## Docs

| Doc | Contents |
| --- | --- |
| [Using the app](docs/using.md) | Connect with GitHub, screens, in-app links |
| [Architecture](docs/architecture.md) | Packages, backends, GitHub API |
| [Development](docs/development.md) | Build, run, test, project layout |
| [Build automation](docs/build-automation.md) | GitHub Actions, workflow permissions, signing secrets |
| [Releases](docs/releases.md) | SemVer 2.0, local build + publish walkthrough, GitHub Releases |
| [F-Droid / Droidify](docs/fdroid.md) | Same catalog as Burton Sonos, Fingerprint, setup + Pages publish |
| [Contributing](CONTRIBUTING.md) | Conventional Commits (required) |

## Quick start (debug)

```bash
./gradlew :app:installDebug
```

Debug builds use application id `com.burton.issues.debug`. Release builds need a keystore; see [docs/releases.md](docs/releases.md).

```bash
./gradlew testDebugUnitTest
```

One-time F-Droid and GitHub signing setup:

```bash
./scripts/setup-fdroid-and-secrets.sh
```

## License and scope

This is a household issue tracker. It does not replace github.com for project boards, merge queues, or org administration.

# Architecture

The app is a single Gradle module (`:app`), Kotlin, Jetpack Compose, Hilt, OkHttp, Coil, and DataStore. UI collects `IssuesRepository` state.

```
ui/          Compose screens and ViewModels (Hilt)
domain/      Account, TrackedApp, IssueSummary, IssueComment, NewIssueRequest
data/
  backend    IssueTracker (host-agnostic)
  github     GitHubApi, GitHubTracker, OAuth (device + PKCE), access token
  parse      TinyJson, GitHubCodec, GradleIds / Android catalog
  deeplink   burtonissues://new, CREATE_ISSUE, ACTION_SEND
  repository IssuesRepository, LocalPrefs (DataStore), InstalledApps
di/          OkHttp, Coil ImageLoader, GitHubTracker binding
```

## Backends

`IssueTracker` is the seam for multiple issue hosts. `GitHubTracker` is bound in Hilt today. A GitLab (or other) tracker can implement the same methods without changing screens. Settings shows the active backend name.

## Auth

**Connect with GitHub** starts the [device flow](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow) (`login/device/code` + poll `login/oauth/access_token`). The user confirms a short code in a Custom Tab. If device login cannot start, the app falls back to authorization-code + PKCE. GitHub requires an HTTPS callback for that path, so it returns to `https://burton-workspaces.github.io/burton-issues/oauth/?code=…`. That static page (`web/oauth/index.html`) hops to `burtonissues://oauth`. There is no client secret in the APK. Scopes: `public_repo read:user read:org`. Tokens live in DataStore (`burton_issues`) as `user_token`. Paste-token sign-in stores the same field.

`users` (`GET /user`) fills the account snapshot. Screens never see the token string after sign-in; the repository holds it in memory and DataStore. OkHttp adds `Authorization: Bearer` for `api.github.com`.

## Catalog

`GET /orgs/Burton-Workspaces/repos?type=public` lists public repos. `AndroidCatalog` keeps Kotlin/Java Android apps and drops sites, F-Droid trees, and `rabun` / `rgit` prefixes. `applicationId` is read from `app/build.gradle.kts` (contents API). Installed `com.burton.*` packages are matched through PackageManager (launcher queries). Subscriptions are a newline-separated repo list in DataStore; first launch subscribes to the full catalog.

## Issues

`GitHubApi` GETs and POSTs JSON to `https://api.github.com/{path}` with `Accept: application/vnd.github+json`. Responses are TinyJson maps. GitHubCodec maps those onto domain models. Pull requests (`pull_request` on the issue object) are dropped.

| Path | Use |
| --- | --- |
| `user` | Signed-in account |
| `orgs/Burton-Workspaces/repos` | Public catalog |
| `repos/{repo}/issues` | List / create |
| `repos/{repo}/issues/{n}` | Get / patch (close, labels, assignees, milestone, title, body) |
| `repos/{repo}/issues/{n}/comments` | List / add |
| `repos/{repo}/issues/comments/{id}` | Edit / delete |
| `repos/{repo}/labels` `assignees` `milestones` | Pickers |
| `search/issues` | Inbox + Search |

Inbox search is `repo:A repo:B is:issue is:open`.

## In-app links

`MainActivity` is `singleTask`. `burtonissues://new`, `https://burton-workspaces.github.io/burton-issues/new`, `ACTION_SEND`, and `com.burton.issues.action.CREATE_ISSUE` queue a `NewIssueRequest`. After sign-in, Compose opens with one app selected in the dropdown (from the link, extras, or the calling package).

## UI shell

`MainActivity` hosts a `NavHost`. **Inbox**, **Apps**, and **Search** are bottom tabs. Apps rows open that app’s issue list. Issue list, issue, and compose are stacked routes and hide the tab bar. Settings is a full-screen modal (tracked apps, about, account, sign out). Sign-in is a gate when no token is stored.

Theme tokens match Burton Sonos: black surfaces, ivory text, sand accent, danger `#C45C4A`. Empty lists use the Burton empty-state card (`EmptyStatePanel`).

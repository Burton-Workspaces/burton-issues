# Using Burton Issues

Burton Issues is an account client. Sign in with **Connect with GitHub**. The token is stored on the phone. Issues go through GitHub’s HTTPS API.

## Connect

1. Tap **Connect with GitHub** and allow the Burton Issues application in the browser.
2. GitHub shows a short device code. The app displays the same code. Confirm it, then the token stays on the phone (DataStore).

The GitHub OAuth app is registered for org **Burton-Workspaces**. GitHub’s browser flow also accepts an **HTTPS** callback, so register:

`https://burton-workspaces.github.io/burton-issues/oauth/`

That Pages hop (`web/oauth/index.html`) opens `burtonissues://oauth` with the authorization code (PKCE). Someone has to create the OAuth app once, then put the public Client ID in `github/client-id.txt`. After that, every phone uses the same application. Never put a client secret in the APK.

Sign out from Settings. That deletes the token from the phone.

### Use a token (optional)

On Connect, **Use a token** pastes a classic PAT (`public_repo`) or a fine-grained token that can read and write issues on Burton-Workspaces. Prefer Connect with GitHub when the Client ID is set.

## Screens

### Connect

Shown when no token is stored. **Connect with GitHub** starts GitHub device login (and falls back to the HTTPS callback). Failed login stays on this screen with an error and retry. **Use a token** is a debug fallback.

### Inbox

Open issues across the apps you subscribe to. Tap a row to open it. The plus control files a new issue. Settings (gear) holds the account, backend, sign out, and the app version.

If nothing is subscribed, the empty-state card is **Nothing subscribed yet** with **Choose apps**. If subscribed apps have no open issues, the card is **Everything is up to date**.

### Apps

Public Android repositories under [Burton-Workspaces](https://github.com/Burton-Workspaces). The list is refreshed from GitHub (Kotlin/Java Android apps). Check the apps you want in Inbox. Installed packages on the phone are marked. Tap a row for that app’s issue list.

### Issue list

Open / Closed / All. Plus files a new issue for that app.

### Issue

Title, body, labels, assignees, milestone, comments. **Edit** changes title and body. **Close** / **Reopen**. **Comment** posts. Delete a comment if GitHub allows it.

### Search

GitHub issue search over subscribed repos. Tap a hit to open it.

### New issue from another app

Other Android apps can open a compose screen for a particular installed app:

```
burtonissues://new?package=com.burton.pod
burtonissues://new?repo=Burton-Workspaces/burton-pod&title=Crash
https://burton-workspaces.github.io/burton-issues/new?package=com.burton.pod
```

Or an explicit intent:

```
Intent("com.burton.issues.action.CREATE_ISSUE")
  .putExtra("package", "com.burton.pod")
  .putExtra("title", "Crash on launch")
  .putExtra("body", "…")
```

Query keys: `package`, `repo`, `app`, `title`, `body`.

## Permissions

| Android | Permission | Why |
| --- | --- |
| All | Internet | GitHub API |

No location or nearby-devices permission. Debug builds use application id `com.burton.issues.debug` and can sit next to a signed install.

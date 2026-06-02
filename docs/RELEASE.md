# SecureVault Release Workflow

This project publishes release artifacts through GitHub Actions.

## Release Gates

The workflow only builds release artifacts after the validation job passes.
The validation job runs on `windows-latest` and checks the active project scope:

- `:shared:common:desktopTest`
- `:desktopApp:desktopTest`
- `:androidApp:testDebugUnitTest`
- `:androidApp:assembleDebug`
- `:androidApp:assembleRelease`
- `detekt`

If any gate fails, Android and Windows packaging jobs do not run, and no GitHub Release is published.

## Triggers

- Pull requests to `main` or `master`: run validation gates only.
- Pushes to `main` or `master`: run validation gates only.
- Pushes of tags matching `v*`: run validation gates, build artifacts, and publish a GitHub Release.
- Manual `workflow_dispatch`: build artifacts; publish only when run from a `v*` tag with `publish=true`.

## Creating A Release

Create and push a version tag:

```powershell
git tag v1.0.0
git push origin v1.0.0
```

When the workflow passes, GitHub Release assets include:

- Android release APK from `androidApp`
- Windows Desktop MSI from `desktopApp`

## Android Signing

The current workflow builds `:androidApp:assembleRelease`. This verifies the Android release build and R8 path, but the project does not currently define CI signing secrets or a release signing configuration.

Before distributing Android builds to end users, add a signing configuration backed by GitHub Actions secrets, then ensure the workflow publishes the signed release APK or bundle.

# SecureVault UI Design Specification

> Version 2.0  
> Updated: 2026-03-19  
> Scope: Compose Multiplatform UI in `composeApp`

This document is the UI source of truth for SecureVault. It is intentionally practical, token-based, and implementation-ready. New screens and refactors should follow this document before inventing new patterns.

## Product Direction

SecureVault should feel like:

- Bitwarden in structure: vault-first information architecture, search and filter first, fast access to entry details, generator and settings as top-level destinations.
- 1Password in polish: layered surfaces, calm spacing, refined focus states, smooth but restrained motion, premium detail work.
- Balanced in emphasis: one clearly primary action per screen, but not an app that shouts. Secondary actions remain visible, useful, and easy to reach.

The app should communicate three things immediately:

1. Your data is protected.
2. The UI is fast to scan and act on.
3. The app is polished without becoming decorative.

## Implementation Contract

All app UI must be built inside `AppTheme(themeMode = ...)`.

Use these theme access points instead of hardcoded values:

- `MaterialTheme.colorScheme`
- `MaterialTheme.typography`
- `MaterialTheme.shapes`
- `MaterialTheme.spacing`
- `MaterialTheme.elevation`
- `MaterialTheme.radius`
- `MaterialTheme.layout`

Default component stack:

- Buttons: `MyAppButton`
- Cards: `MyAppCard`
- Inputs: `MyAppInput`
- Top bars: `MyAppTopBar`
- Rows and settings items: `MyAppListItem`
- Domain-specific building blocks: `PasswordCard`, `DetailRow`, `PasswordStrengthBar`, `OptionSwitchRow`, `CountStepperRow`

Rule: prefer `MyApp*` components for all new work. Legacy `Sv*` components should only remain where there is no equivalent yet or where migration is intentionally deferred.

## Design Principles

### 1. Secure By Default

- Never reveal secrets by default.
- Use visual signals for protected states: lock icons, security badges, confirmation dialogs, screenshot protection, biometric settings.
- Sensitive actions must feel deliberate, not casual.

### 2. Fast To Scan

- Users should understand each screen in under 3 seconds.
- Use a strong text hierarchy: title, supporting text, metadata.
- Search, filter, copy, reveal, and save must be easy to find without hunting.

### 3. Calm Premium Polish

- Prefer subtle elevation, surface layering, and animated state changes over flashy gradients or novelty effects.
- Motion should support comprehension, not entertainment.
- Keep the palette restrained and security-oriented.

### 4. Balanced Action Emphasis

- Each screen gets one dominant action.
- Supporting actions remain visible but should not compete with the primary action.
- Dangerous actions should be separated visually and confirmed.

### 5. Cross-Platform Consistency

- The same task should look and behave similarly on Android, iOS, and Desktop.
- Platform adaptation is welcome, but only when it improves usability without fragmenting the design system.

### 6. Accessibility Is Non-Negotiable

- Minimum touch target: `48.dp`
- Body text contrast target: WCAG AA
- Focus, pressed, error, and selected states must all be visible without relying on color alone

## App Structure

Top-level app structure should remain:

- `Vault`
- `Generator`
- `Settings`

Authentication flow should remain separate from the main vault flow:

- `Onboarding`
- `Register`
- `Login`

Core structural pattern:

- Top bar at the top of the content column
- Centered single-column content
- Max readable width: `720.dp`
- Main horizontal padding: `16.dp`
- One main action surface per screen: button bar, FAB, or hero button

This is the intended mental model:

- Vault = browse and retrieve
- Detail = inspect and act
- Add/Edit = author and save
- Generator = create and copy
- Settings = configure trust and behavior

## Primary Action Model

Use this action hierarchy consistently.

| Level | Component | Typical use |
|------|------|------|
| Primary | `MyAppButton(variant = Primary)` | Unlock, Create vault, Save, Edit |
| Secondary | `MyAppButton(variant = Secondary)` | Cancel, Generate password, Delete when paired with a primary |
| Low emphasis | `MyAppButton(variant = Ghost/Text)` | Biometric login, alternate auth path, helper actions |
| Dangerous | `MyAppButton(variant = Danger)` | Lock now, irreversible destructive actions when they are the main decision |

Rules:

- Use only one filled primary button in the same viewport region.
- If a bottom action bar already has a primary button, do not add another filled CTA in the content area.
- If a FAB exists, it is the primary action for that screen.
- Do not place two equally strong filled buttons side by side.
- Destructive actions should usually be `Secondary` plus a confirmation dialog, unless the screen itself is a danger flow.

## Design Tokens

### Color System

SecureVault uses a green-led Material 3 palette with semantic security accents.

#### Core Light and Dark Colors

| Token | Light | Dark | Use |
|------|------|------|------|
| `primary` | `#1B6B4F` | `#7DDCB5` | Main CTA, active states, positive emphasis |
| `onPrimary` | `#FFFFFF` | `#003828` | Text and icons on primary |
| `primaryContainer` | `#A4F4D2` | `#005138` | FAB, highlighted controls, positive containers |
| `onPrimaryContainer` | `#002114` | `#A4F4D2` | Content on primary container |
| `secondary` | `#4A635A` | `#B3CCC1` | Supporting emphasis, non-primary accents |
| `secondaryContainer` | `#CCE8DC` | `#334B42` | Alternate container layers |
| `tertiary` | `#3A6472` | `#A2CEDE` | Reserved for informational accents |
| `error` | `#BA1A1A` | `#FFB4AB` | Errors, destructive states |
| `errorContainer` | `#FFDAD6` | `#93000A` | Error backgrounds |
| `background` | `#F8FAF7` | `#191C1B` | App background |
| `surface` | `#F8FAF7` | `#191C1B` | Base surfaces |
| `surfaceVariant` | `#DBE5DE` | `#404944` | Muted surfaces |
| `surfaceContainerLow` | `#F2F4F1` | `#1D201F` | Resting input fill |
| `surfaceContainer` | `#ECEFEB` | `#212524` | Filled cards, focused inputs |
| `surfaceContainerHigh` | `#E6E9E5` | `#2C302E` | Higher surface layer |
| `surfaceContainerHighest` | `#E1E3E0` | `#363A38` | Icon chips and strongest neutral layer |
| `outline` | `#707973` | `#8A938D` | Borders, strokes |
| `outlineVariant` | `#BFC9C2` | `#404944` | Soft dividers, subtle borders |
| `inverseSurface` | `#2E312F` | `#E1E3E0` | Toasts or inverse surfaces |

#### Semantic Functional Colors

| Token | Value | Use |
|------|------|------|
| `StrengthVeryWeak` | `#D32F2F` | Password strength: very weak |
| `StrengthWeak` | `#F57C00` | Password strength: weak |
| `StrengthMedium` | `#FBC02D` | Password strength: medium |
| `StrengthStrong` | `#388E3C` | Password strength: strong |
| `StrengthVeryStrong` | `#1B6B4F` | Password strength: very strong |
| `SecurityModeColor` | `#7B1FA2` | Security mode badge and notice |
| `SecurityModeContainerLight` | `#F3E5F5` | Optional security mode container in light theme |
| `SecurityModeContainerDark` | `#4A148C` | Optional security mode container in dark theme |
| `FavoriteColor` | `#FFB300` | Favorite star accent |
| `PasswordDisplayBackground` | `#1A1A2E` | Reserved for protected password display surfaces |

### Dynamic Color Strategy

Use dynamic color only where already supported by `AppTheme`:

- Android 12+: dynamic scheme may override fallback scheme
- Other platforms: use SecureVault fallback palette

Rule: document and design against fallback tokens first. Dynamic color is an enhancement, not the baseline specification.

### Typography

Base font family:

- App UI: `FontFamily.Default`
- Password/plain secret display: `PasswordFontFamily = FontFamily.Monospace`

| Token | Size | Line height | Weight | Use |
|------|------|------|------|------|
| `headlineLarge` | `28.sp` | `36.sp` | Normal | App hero title, auth title |
| `headlineMedium` | `24.sp` | `32.sp` | Normal | Large section title |
| `headlineSmall` | `22.sp` | `28.sp` | Normal | Top bar title, important section heading |
| `titleLarge` | `20.sp` | `28.sp` | Medium | Card title when larger emphasis is needed |
| `titleMedium` | `16.sp` | `24.sp` | Medium | List item headline, field grouping |
| `titleSmall` | `14.sp` | `20.sp` | Medium | Section labels like "Basic info" |
| `bodyLarge` | `16.sp` | `24.sp` | Normal | Main field content |
| `bodyMedium` | `14.sp` | `20.sp` | Normal | Supporting text, helper content |
| `bodySmall` | `12.sp` | `16.sp` | Normal | Metadata, hints, badge-adjacent content |
| `labelLarge` | `14.sp` | `20.sp` | Medium | Button labels |
| `labelMedium` | `12.sp` | `16.sp` | Medium | Compact controls |
| `labelSmall` | `11.sp` | `16.sp` | Medium | Badges and compact status labels |

Guidance:

- Never style body text larger than `bodyLarge` just to create emphasis. Use title styles instead.
- Show revealed passwords with `PasswordFontFamily`.
- Hidden passwords should stay in standard body typography with bullet masking.

### Spacing

`Spacing` tokens:

| Token | Value |
|------|------|
| `xs` | `4.dp` |
| `sm` | `8.dp` |
| `md` | `16.dp` |
| `lg` | `24.dp` |
| `xl` | `32.dp` |
| `xxl` | `48.dp` |

Spacing rules:

- Use `4.dp` only inside tightly packed controls.
- `8.dp` is the standard small gap between related elements.
- `16.dp` is the default content spacing.
- `24.dp` separates sections.
- `32.dp` and above are reserved for auth and empty-state breathing room.

### Radius and Shape

`Radius` tokens:

| Token | Value |
|------|------|
| `xs` | `4.dp` |
| `sm` | `8.dp` |
| `md` | `12.dp` |
| `lg` | `16.dp` |
| `xl` | `28.dp` |

`SecureVaultShapes` mapping:

- `extraSmall` = `4.dp`
- `small` = `8.dp`
- `medium` = `12.dp`
- `large` = `16.dp`
- `extraLarge` = `28.dp`

Rules:

- Filled and elevated cards use `medium` by default.
- Buttons and inputs use `large`.
- Security badges and compact chips use `extraSmall` or `small`.

### Elevation

| Token | Value | Use |
|------|------|------|
| `none` | `0.dp` | Flat surfaces |
| `low` | `1.dp` | Elevated card resting state |
| `medium` | `3.dp` | Primary button resting state |
| `high` | `6.dp` | Reserved for stronger lift |
| `overlay` | `8.dp` | Floating UI and overlays |

Rules:

- Use tonal layering first, elevation second.
- Most content should stay between `0.dp` and `3.dp`.
- A password manager should feel stable and grounded, not floaty.

### Layout Tokens

| Token | Value | Use |
|------|------|------|
| `minInteractiveSize` | `48.dp` | Minimum touch target |
| `buttonHeight` | `48.dp` | All `MyAppButton` variants |
| `buttonIconSize` | `18.dp` | Button leading icon and spinner size basis |
| `buttonProgressStrokeWidth` | `2.dp` | Loading spinner stroke |
| `inputHeight` | `56.dp` | Single-line input minimum |
| `heroIconSize` | `64.dp` | Auth and empty-state icons |
| `listItemIconContainerSize` | `40.dp` | Leading circle in `PasswordCard` |
| `listItemIconSize` | `20.dp` | Leading card icon |
| `smallStatusIconSize` | `16.dp` | Favorite star and status icons |
| `pageHorizontalPadding` | `16.dp` | Main content edge padding |
| `pageMaxWidth` | `720.dp` | Standard centered content width |
| `pageTopPadding` | `16.dp` | General top content spacing |
| `sectionSpacing` | `24.dp` | Major vertical sections |
| `contentSpacing` | `16.dp` | Default stacked spacing |
| `compactContentSpacing` | `12.dp` | Denser list spacing |
| `cardPaddingHorizontal` | `16.dp` | Card internal horizontal padding |
| `cardPaddingVertical` | `12.dp` | Card internal vertical padding |
| `topBarTopPadding` | `12.dp` | Top bar top spacing |
| `topBarBottomPadding` | `8.dp` | Top bar bottom spacing |
| `topBarSideWidth` | `48.dp` | Reserved side action width in top bar |
| `badgeHorizontalPadding` | `6.dp` | Badge horizontal inset |
| `badgeVerticalPadding` | `2.dp` | Badge vertical inset |
| `fabContentClearance` | `88.dp` | Extra list bottom space when FAB exists |
| `bottomBarActionInset` | `96.dp` | Bottom sticky action clearance |
| `fabElevation` | `8.dp` | FAB resting elevation |
| `fabPressedElevation` | `3.dp` | FAB pressed elevation |

## Layout Rules

### Screen Shell

Default shell for most screens:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = MaterialTheme.layout.pageMaxWidth)
            .align(Alignment.TopCenter)
            .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.contentSpacing),
    ) {
        MyAppTopBar(title = "Screen")
        // content
    }
}
```

Rules:

- Center content horizontally on larger screens.
- Do not let form screens stretch edge to edge on desktop.
- Keep scrolling content separate from sticky bottom actions.

### Responsive Guidance

Use these working breakpoints:

| Width | Behavior |
|------|------|
| `< 600.dp` | Compact single-column layout |
| `600.dp - 839.dp` | Wider single-column layout, still centered |
| `>= 840.dp` | Keep centered content by default; only adopt split-pane patterns intentionally |

Implementation guidance:

- Current baseline is a centered single-column layout with `pageMaxWidth = 720.dp`.
- If a future split view is added for Vault on expanded screens, preserve the same card, token, and action hierarchy. Do not create a second design language for desktop.

## Component Guidelines

### `MyAppButton`

Use `MyAppButton` as the default button abstraction.

#### Primary

- Filled background using `colorScheme.primary`
- Use for the one main action on the screen
- Examples: unlock, create vault, save, edit

#### Secondary

- Outlined button using `outlineVariant`
- Use for visible but non-dominant actions
- Examples: generate password, cancel, delete when paired with edit/save

#### Ghost / Text

- Borderless button using `onSurfaceVariant`
- Use for alternate flows and helper actions
- Examples: biometric login, "Already have an account?", lightweight utilities

#### Danger

- Filled error-colored button
- Use only when the action is genuinely destructive or security-critical
- Example: lock now

Rules:

- Do not create custom button heights. Use `MaterialTheme.layout.buttonHeight`.
- Use leading icons only when they improve recognition.
- Keep labels short and verb-first.
- Loading state is allowed only for filled variants in the current API.

### `MyAppCard`

Use `MyAppCard` for grouped information and interactive surfaces.

| Variant | Use |
|------|------|
| `Filled` | Default grouped content, form sections, detail surfaces |
| `Elevated` | Clickable entry cards in the vault |
| `Outlined` | Rare cases where stronger separation is needed without fill |

Rules:

- Default card content padding should come from the component.
- Pass `PaddingValues(0.dp)` only when the child layout owns its own padding.
- Use elevated cards primarily for tappable list items, not every container.

### `MyAppInput`

`MyAppInput` is the default text field for forms and search.

Behavior already built in:

- animated border and label tint
- subtle focus halo
- error reveal animation
- password visibility toggle
- optional supporting and error text

Rules:

- Search should use `leadingIcon = Search`.
- Password fields should use `isPassword = true`.
- Keep labels explicit and noun-based: `Title`, `Username`, `Master password`.
- Supporting text should clarify, not repeat the label.
- Error text should be brief and actionable.

### `MyAppTopBar`

Use `MyAppTopBar` for screen-level titles.

Rules:

- Keep titles short, ideally 2 to 6 words.
- Back affordance appears only when leaving the current flow.
- Use action icons sparingly; if the screen already has a sticky action bar, do not overload the top bar.

### `MyAppListItem`

Use for settings rows, detail rows, and structured lists.

Rules:

- `headline` is the main value or title.
- `supportingText` is explanatory, not redundant.
- `overlineText` is useful for field labels in detail views.
- For grouped settings, wrap multiple list items in one `MyAppCard` with dividers.

### Domain Components

#### `PasswordCard`

Use as the default vault entry row.

Required contents:

- title
- username if present
- URL if present
- favorite indicator if favorite
- security badge if `securityMode == true`
- lock-based leading icon container

Rules:

- Keep the card tappable as one whole surface.
- Do not show password values in the vault list.
- URL is tertiary information and should truncate to one line.
- Favorite and security mode should sit in the trailing column, not compete with the title.

#### `DetailRow`

Use for stable read-only fields in the detail screen.

Rules:

- Use `overlineText` for the field label.
- Allow copy only where it is genuinely useful.
- Copy feedback should switch icon state to `Check` and then reset automatically.

#### `PasswordStrengthBar`

Use on registration and password editing whenever the user authors a password.

Strength mapping:

| Level | Color |
|------|------|
| Very weak | `StrengthVeryWeak` |
| Weak | `StrengthWeak` |
| Medium | `StrengthMedium` |
| Strong | `StrengthStrong` |
| Very strong | `StrengthVeryStrong` |

Rules:

- Always pair color with text.
- Never use color alone to communicate strength.

#### `OptionSwitchRow` and `SettingsSwitchRow`

Use for boolean settings and form toggles.

Rules:

- Use concise labels.
- Add descriptions only when the impact is not obvious.
- Security-affecting toggles should explain the consequence clearly.

#### `CountStepperRow`

Use for bounded numeric adjustments in the generator.

Rules:

- Prefer steppers when the range is small and meaningful.
- Keep `+` and `-` buttons at `48.dp`.
- Show the current number as stable centered text.

## Screen-Specific Guidance

### Login

Primary goal: unlock the vault fast and safely.

Required hierarchy:

- hero icon and product name
- master password input
- primary unlock button
- optional biometric action
- alternate register action

Rules:

- The unlock button is the only filled CTA.
- Biometric login is low-emphasis, not equal emphasis.
- Error text appears directly above the primary action.

### Register

Primary goal: create a vault with confidence.

Required hierarchy:

- title and explanatory subtitle
- master password input
- strength bar and hints
- confirmation input
- primary create button
- alternate login action

Rules:

- Password guidance must be visible before submission.
- Confirmation mismatch should appear inline.
- Keep the main action pinned to the form flow, not hidden after long content.

### Vault

Primary goal: search, filter, and open entries quickly.

Required hierarchy:

- top bar title
- search input
- filter row
- vault list or empty state
- add-entry FAB

Rules:

- Search appears above filters.
- Filters should be horizontally scannable.
- The add FAB is the screen's primary action.
- Empty state should reassure and guide the first action.

### Password Detail

Primary goal: inspect an entry and perform safe actions.

Required hierarchy:

- top bar
- security mode notice when enabled
- detail rows
- password row with reveal or copy/use actions
- sticky bottom action bar

Rules:

- Use a sticky bottom bar for `Edit` and `Delete`.
- `Edit` is the primary action.
- `Delete` must trigger a confirmation dialog.
- Security mode removes password reveal.

### Add/Edit Password

Primary goal: author or revise an entry without friction.

Required hierarchy:

- top bar
- basic info section
- password field and generator helper
- strength bar
- optional metadata
- options section
- sticky save button

Rules:

- Save stays disabled until required fields are present.
- Use `MyAppInput` for all text-entry fields.
- The password generator helper should be secondary emphasis.

### Generator

Primary goal: create a usable password fast, then fine-tune if needed.

Required hierarchy:

- generated password output
- copy and regenerate actions
- quick presets
- custom controls

Rules:

- The generated result must be visually prominent and monospace.
- Regenerate and copy should be adjacent to the generated value.
- Presets should be faster than manual tuning.

### Settings

Primary goal: configure trust, appearance, and lock behavior.

Required hierarchy:

- appearance section
- security section
- lock action

Rules:

- Theme selection uses grouped radio rows.
- Security settings live together in a single mental cluster.
- `Lock now` may use `Danger` because it is an immediate high-salience security action, not a destructive data action.

## Motion Rules

Motion must feel fast, polished, and quiet.

### Use the Shared Tokens

Use `AnimationTokens` and `NavTransitions` instead of ad hoc numbers.

| Token | Value | Use |
|------|------|------|
| `pageEnterDuration` | `280` ms | Screen entry |
| `pageExitDuration` | `220` ms | Screen exit |
| `cardAppearDuration` | `220` ms | Item entrance |
| `dialogDuration` | `240` ms | Dialog reveal |
| `crossFadeDuration` | `160` ms | Small state swaps |
| `copyFeedbackDuration` | `260` ms | Copy icon tint change |
| `strengthBarDuration` | `360` ms | Strength transitions |
| `unlockDuration` | `500` ms | Reserved for unlock success flows |
| `staggerItemDelay` | `36` ms | Staggered item entrance |
| `itemEntranceOffsetPx` | `16` px | List item vertical offset |

Easing:

- `easeOut` for entering
- `easeIn` for exiting
- `easeInOut` for balanced state changes
- `easeOutBack` only for dialog-like reveal moments

### Navigation Motion

| Navigation type | Enter | Exit |
|------|------|------|
| Forward | `fadeIn + slideInHorizontally(+1/8)` | `fadeOut + slideOutHorizontally(-1/8)` |
| Back | `fadeIn + slideInHorizontally(-1/8)` | `fadeOut + slideOutHorizontally(+1/8)` |
| Tab switch | `fadeIn` | `fadeOut` |

Rules:

- Tab changes should feel lighter than stack navigation.
- Avoid large travel distances. Password managers benefit from composure, not drama.

### Component Motion

- Buttons: press-scale to `0.985f` with `buttonPressSpring`
- Cards: subtle shadow change on press for elevated cards
- Inputs: animated border, label, and container change over `180ms`
- Copy feedback: icon changes to `Check`, tint animates to `primary`, then resets after about `1.5s`
- Dialogs: fade and scale in, not bounce-heavy theatrics

### Reduced Motion

If platform support is available, reduce motion should:

- remove slide transitions
- keep fade-only or instant state changes
- disable decorative stagger where it slows scanning

Never use:

- infinite decorative motion
- bouncing icons on idle surfaces
- long loading animations that block action

### Loading

Prefer skeletons over spinners for first-load content.

Use:

- `SkeletonList`
- `SkeletonCard`

Use spinners only inside buttons or very small inline operations.

## Password Manager UX Rules

### Secret Handling

- Passwords are hidden by default.
- Revealed passwords use monospace text.
- Security mode entries never reveal the password.
- Vault list items never preview password content.

### Copy Behavior

- Copy actions must give immediate visible feedback.
- Current implementation auto-clears the clipboard after `30` seconds. The UI should state this when relevant.
- Use snackbar or inline feedback for copied secrets and usernames.

### Security Mode

Security mode is a stricter entry state, not just a badge.

Rules:

- show a clear notice at the top of the detail screen
- show a security badge in the vault list
- disable reveal affordance
- rename the password action semantically from "Copy" to "Use" where it improves clarity

### Search and Retrieval

- Search is a first-class action, not buried behind an icon-only affordance.
- Filters should expose common retrieval paths such as favorites and category.
- Entry rows should optimize for recognition: title first, username second, URL third.

### Screenshot and Privacy

- Screenshot protection is enabled by default through settings state.
- If screenshots are allowed, expose that as an intentional user-controlled choice.
- Do not add share/export affordances casually on secret screens.

### Biometrics

- Biometrics accelerate unlock; they do not replace the master password model.
- Present biometric entry as a secondary path, not as the dominant brand message.

### Destructive Flows

- Deletion must always require explicit confirmation.
- Locking the vault may be immediate, but should still feel intentional through placement and color.

## Dark Mode

Dark mode should feel rich and low-glare, not pure black and neon.

Rules:

- Use the provided dark surfaces instead of absolute black.
- Layer dark surfaces with `surface`, `surfaceContainerLow`, `surfaceContainer`, and `surfaceContainerHigh`.
- Keep borders subtle but still visible with `outline` and `outlineVariant`.
- Maintain the same spacing and motion rhythm as light mode.
- Preserve semantic accents: green for primary trust, purple for security mode, amber for favorites, red for errors.

Dark mode guidance by surface:

| Surface purpose | Recommended token |
|------|------|
| App background | `colorScheme.background` |
| Filled card | `colorScheme.surfaceContainer` |
| Resting text field | `colorScheme.surfaceContainerLow` |
| Focused text field | `colorScheme.surfaceContainer` |
| Elevated neutral layer | `colorScheme.surfaceContainerHigh` |
| Strongest neutral icon chip | `colorScheme.surfaceContainerHighest` |

Do not:

- increase saturation just because the theme is dark
- use transparent white borders that disappear
- make every card elevated in dark mode

## Accessibility Rules

- Minimum target size is `48.dp`
- Text and icon states must remain legible in both themes
- Buttons, switches, radios, and icon buttons need clear labels or semantic descriptions
- Screen-reader text for secrets should describe state, not read the secret
- Use text plus color for password strength and security mode
- Desktop flows should remain keyboard navigable

Recommended semantics for sensitive UI:

```kotlin
Modifier.semantics {
    contentDescription = "Password hidden. Use copy button to copy securely."
}
```

## Do and Don't

### Do

- Use `AppTheme` and theme tokens for all new UI
- Keep one primary action per screen or action bar
- Use `MyAppButton`, `MyAppCard`, `MyAppInput`, and `MyAppTopBar` as the base kit
- Keep vault entry cards information-dense but visually calm
- Use soft surface layering before adding elevation
- Confirm irreversible actions
- Tell the user when copied data will auto-clear
- Use monospace only for visible secret values
- Respect reduced-motion needs

### Don't

- Hardcode colors, spacing, or corner radii in screen code
- Put two filled primary actions side by side
- Reveal passwords by default
- Add decorative gradients, glass effects, or glowing neon surfaces
- Use infinite animation outside loading placeholders
- Hide search behind a secondary screen
- Use red for routine actions or green for every accent
- Mix `MyApp*` and ad hoc components on the same screen without a reason
- Let destructive actions visually overpower the main task

## Compose Implementation Checklist

Before shipping a new screen or refactor, verify:

- wrapped in `AppTheme`
- uses `MaterialTheme.colorScheme`, `typography`, `spacing`, `layout`, `radius`, and `elevation`
- primary action hierarchy is clear
- secrets are hidden by default
- dark mode works without manual overrides
- loading, error, empty, and success states are all designed
- copy feedback exists where copy is available
- destructive actions are confirmed
- touch targets are at least `48.dp`
- motion uses shared tokens instead of custom timings

## Recommended Default Pattern

Use this as the default recipe for new SecureVault screens:

```kotlin
@Composable
fun SecureVaultScreenShell(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = MaterialTheme.layout.pageMaxWidth)
            .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.contentSpacing),
    ) {
        MyAppTopBar(title = title, onBack = onBack)
        content()
    }
}
```

That pattern should be the default until there is a strong product reason to do something else.

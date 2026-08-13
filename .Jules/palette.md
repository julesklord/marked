## 2024-11-20 - Clear Search Icon Accessibility and Interaction Pattern
**Learning:** Found an interaction improvement opportunity in `SidebarContent.kt` where the search bar lacked a way to easily clear the search text. Adding a trailing clear icon that only appears when text is entered improves the user experience by reducing the effort to clear a long search query. Also ensured it had a proper `contentDescription` for accessibility.
**Action:** When adding search bars in Jetpack Compose, always consider adding a trailing clear icon button for better UX, and ensure it's accessible with a localized content description.
## 2024-06-25 - Dialogue Interaction Improvements
**Learning:** We realized that users often fail to notice whether a text input in a dialog modal is empty, causing confusing invalid states when creating or renaming notes. We also saw that missing an `ImeAction.Done` handler broke expected keyboard submit behaviors.
**Action:** Always add empty state handlers to disable the confirm button if the text input is missing. Further, provide an IME action `KeyboardOptions(imeAction = ImeAction.Done)` with an associated `KeyboardActions` handler to submit on "Enter" out of the box.
## 2025-02-12 - Accessibility labels for Jetpack Compose Icons\n**Learning:** When adding accessibility descriptions (like contentDescription for icons) in a multi-language Android app, always ensure new string resources are translated in all values-* directories (e.g., pt, ru, zh) to prevent build-time linting errors (MissingTranslation).\n**Action:** Add translations across all locales when introducing new strings for accessibility.
## 2024-05-18 - Accessibility labels for Jetpack Compose Icons (Search)
**Learning:** Found an accessibility opportunity in `MainActivity.kt` and `SidebarContent.kt` where the search icon inside the OutlinedTextField lacked a `contentDescription`. Providing a localized string resource for the icon's description improves screen reader experience. Always ensure translations for the new string resource are added across all supported locales (values-* directories) to avoid lint errors.
**Action:** When adding icons to Compose UIs, always include an appropriate localized `contentDescription`.
## 2025-02-23 - Submit Search Action Accessibility and Interaction Pattern
**Learning:** Found an interaction improvement opportunity in the Search bar implementations (`SidebarContent.kt` and `MainActivity.kt`). Adding `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)` and `keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })` improves the UX by explicitly handling the keyboard search action instead of leaving it as a newline or unhandled action. This ensures the keyboard dismisses when the user executes a search intent.
**Action:** Always add appropriate `KeyboardOptions` and `KeyboardActions` (e.g., `ImeAction.Search`, `ImeAction.Done`) for inputs to provide expected mobile keyboard interactions.
## 2024-11-23 - Clear Input Accessibility and Interaction Pattern
**Learning:** Added a trailing clear icon button to OutlinedTextField inputs inside dialogs (Create/Rename) for easier query deletion. Verified conditional rendering based on input state and added accessibility descriptions across all supported locales.
**Action:** Remember to add `trailingIcon` clears for user-friendly text input fields in forms and dialogs.

## 2026-07-17 - Duplicated UI components causing UX inconsistencies
**Learning:** Discovered that `MainActivity.kt` contained duplicated UI components (`SidebarContent` and `MarkdownEditorArea`) that were missing recent localization/accessibility strings, whereas the correct files under `ui/components/` had them. The codebase has duplicated files that could lead to UX fragmentation, because fixes applied in one file may be missed in the other.
**Action:** When applying UX or accessibility improvements, be aware of potentially duplicated composables in `MainActivity.kt` vs the `ui/components/` directory. Ensure string resources are used consistently everywhere rather than hardcoded text.
## 2026-07-25 - Extracted hardcoded UI strings into localized resources
**Learning:** Found an accessibility opportunity where hardcoded strings in `MainActivity.kt` (used in UI labels, toolbar buttons, and accessibility tags) were not matching the localized implementation under `ui/components/`. Extracting all these texts using `stringResource` improves UX, ensures language consistency for international users, and provides proper labels for screen readers.
**Action:** When working in Android files like `MainActivity.kt`, never leave user-visible strings hardcoded. Always map them to the proper string references defined in `res/values*/strings.xml` to ensure localization and accessibility standards are met.
## 2024-11-20 - Jetpack Compose Checkbox and Icon Accessibility Patterns
**Learning:** Found two accessibility patterns in Jetpack Compose:
1. When building interactive list items (like a task list), using `.clickable` on the parent row and `onCheckedChange` on the inner Checkbox causes a double ripple effect and confusing screen reader announcements. Using `.toggleable(value, role = Role.Checkbox, onValueChange)` on the row and `onCheckedChange = null` on the checkbox provides clean semantics and interaction.
2. In responsive layouts, keeping an `IconButton` with a no-op click handler just for decorative purposes creates an accessibility anti-pattern (screen readers announce a button that does nothing). It's better to use a non-interactive `Box` with the `Icon`'s `contentDescription = null` for decorative icons.
**Action:** Use `.toggleable` with `Role.Checkbox` for custom checkbox rows, and avoid no-op `IconButton`s for decorative icons by using `Box` and `null` content descriptions instead.
## 2024-11-20 - Redundant Accessibility Labels for Icons
**Learning:** Found an accessibility issue where an `Icon` and its adjacent `Text` label inside a button (`FormatToolbarButton`) were both providing the same label, causing screen readers to read the same information twice (e.g. "Bold, Bold").
**Action:** When creating components with both an `Icon` and `Text`, and the text acts as a visible label for the action, set the `Icon`'s `contentDescription` to `null` to mark it as decorative. This prevents redundant screen reader announcements.
## 2025-05-18 - Redundant Accessibility Labels for Icons in Dropdowns and Buttons
**Learning:** Found an accessibility issue where an `Icon` and its adjacent `Text` label inside a `DropdownMenuItem` or generic `Button` (e.g., "New Document" empty state) were both providing the same label, causing screen readers to read the same information twice.
**Action:** When creating components with both an `Icon` and `Text`, and the text acts as a visible label for the action, set the `Icon`'s `contentDescription` to `null` to mark it as decorative. This prevents redundant screen reader announcements.
## 2026-08-03 - Crossfade State Transitions
**Learning:** Found an interaction improvement opportunity in `MainActivity.kt`. The UI would abruptly snap between the empty state, read mode, and edit mode. Wrapping these main state branches in `androidx.compose.animation.Crossfade` provides a smooth visual transition, greatly enhancing the application's premium feel with minimal code.
**Action:** When conditionally swapping large UI components in Jetpack Compose based on state (like mode toggles or item selection), consider wrapping them in `Crossfade` and passing the state as the `targetState` to ensure smooth transitions.
## 2026-08-04 - Empty State Call-to-Action Pattern
**Learning:** Found a UX opportunity in the sidebar document list (`SidebarContent.kt` and `MainActivity.kt`). When the list of documents or the search result is empty, the user simply saw a "No notes found" text message, leading to a dead end. By adding a clear "New Document" button when there are no notes, and a "Clear search" button when a search yields no results, we create a helpful and actionable empty state.
**Action:** When designing lists or search interfaces in Jetpack Compose, never leave the user at a dead end. Always accompany empty states with an actionable `Button` or `TextButton` (like "Clear Search" or "Create New") to help the user recover.

## 2026-08-08 - Added Tooltips to TopAppBar IconButtons
**Learning:** Found an accessibility improvement in `MainActivity.kt`. Adding tooltips to icon-only buttons on the top app bar using `TooltipBox` enhances the user experience by providing a text description when users long-press the buttons.
**Action:** Always wrap icon-only buttons with `TooltipBox` in Jetpack Compose to provide textual descriptions for better accessibility and UX.
## 2024-05-18 - TooltipBox accessibility improvement
**Learning:** Icon-only buttons lack critical context on desktop/hover interactions. Using Material 3 TooltipBox is the standard accessibility pattern for revealing the contentDescription string resources visually on hover/long-press.
**Action:** Always wrap `IconButton` components in `TooltipBox` to improve a11y, remembering to include `@OptIn(ExperimentalMaterial3Api::class)` when doing so in Jetpack Compose.

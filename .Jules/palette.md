## 2024-05-18 - Compose Search Bar UX
**Learning:** Found that the app's `OutlinedTextField` used for searching documents lacks a clear way to reset the search string quickly. This is a common accessibility/UX pitfall, especially for mobile users where backspacing a long string is tedious.
**Action:** Implemented a conditionally visible clear icon (`trailingIcon`) in `OutlinedTextField` when the text is not empty. This pattern should be standard for all search inputs in this design system.

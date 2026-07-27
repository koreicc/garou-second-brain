---
name: Second Brain
description: A device-owned, local-first personal knowledge system for Android.
colors:
  primary: "#1565C0"
  primary-container: "#D1E4FF"
  secondary: "#545F71"
  secondary-container: "#D8E3F8"
  tertiary: "#6D5676"
  tertiary-container: "#F6D9FF"
  background: "#FDFBFF"
  surface: "#FDFBFF"
  surface-container-low: "#F7F8FB"
  surface-container: "#F0F2F5"
  surface-container-high: "#EBEDF0"
  surface-container-highest: "#DEE0E4"
  on-surface: "#1A1C1E"
  on-surface-variant: "#43474E"
  outline: "#73777F"
  error: "#BA1A1A"
  status-pending: "#9E9E9E"
  status-in-progress: "#FFA726"
  status-completed: "#66BB6A"
  status-expired: "#EF5350"
typography:
  display:
    fontFamily: "Roboto, sans-serif"
  headline:
    fontFamily: "Roboto, sans-serif"
  title:
    fontFamily: "Roboto, sans-serif"
  body:
    fontFamily: "Roboto, sans-serif"
  label:
    fontFamily: "Roboto, sans-serif"
rounded:
  extra-small: "6dp"
  small: "10dp"
  medium: "14dp"
  large: "18dp"
  extra-large: "24dp"
  compact-control: "12dp"
  pill: "50%"
spacing:
  compact: "4dp"
  small: "8dp"
  medium: "16dp"
  roomy: "20dp"
  large: "24dp"
components:
  navigation-pill:
    backgroundColor: "{colors.surface-container-high}"
    rounded: "{rounded.pill}"
    height: "56dp"
  navigation-pill-selected-item:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.pill}"
    height: "48dp"
  floating-action-button:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.pill}"
    size: "56dp"
  status-badge:
    rounded: "{rounded.pill}"
    padding: "3dp 8dp"
---

# Design System: Second Brain

## Overview

**Creative North Star: "Material Yoldaş"**

Second Brain's incumbent visual system is a native Android companion that borrows its character from the user's own device. Material You is the visual authority: Android 12+ wallpaper colors create the preferred palette, while a composed blue fallback protects consistency when system colors are unavailable. The result is intentionally personal rather than externally branded: the user's phone gives the application its atmosphere.

The system combines calm operational surfaces with selective play. Transparent app bars let a soft, wallpaper-aware gradient breathe behind screen content. Rounded tonal containers, a floating navigation pill, detached circular FAB, and medium-bouncy spring transitions create a companion that is personalizable and expressive without abandoning Android fluency. Its visual behavior honors system dark mode, optional OLED black, contrast needs, and reduced-motion settings.

**Key Characteristics:**
- Device-owned color through Material You, curated seeds, and optional custom palette input.
- Standard Material 3 hierarchy with extra personality concentrated in navigation, theme atmosphere, and feedback motion.
- Tonal layering first; small, intentional elevation only for floating controls and actionable containers.
- Soft geometric language: rounded rectangles, circular action buttons, and pill-shaped status/navigation elements.
- Personalization is a product feature, not a decorative afterthought.

## Colors

The palette is device-responsive by default: the static blue fallback is a safety net, while wallpaper-derived or user-selected seeds should be allowed to define the lived color character.

### Primary
- **Fallback Signal Blue:** Used for the primary FAB, selected navigation treatment, primary calls to action, and active task state when dynamic color is unavailable. The dynamic Material You primary role replaces this value whenever Android provides a wallpaper scheme.
- **Primary Atmosphere:** The primary container role provides the soft top wash in the optional gradient background and the selected navigation item's tonal fill.

### Secondary
- **Slate Utility:** Secondary roles support supporting actions, completed task communication, and non-primary structure. The secondary container offers a muted tonal layer without competing with the active primary role.

### Tertiary
- **Plum Annotation:** Tertiary roles distinguish pending states, notes, routines, and secondary moments of identity. Use it as an accent, never as a competing primary action color.

### Neutral
- **Cloud Surface:** Background and surface roles form the reading plane. The system uses a measured ladder from lowest through highest surface containers to separate adjacent regions without drawing lines everywhere.
- **Ink Content:** On-surface roles carry all core reading and interaction text; on-surface-variant is reserved for metadata, secondary labels, and quieter control states.
- **Boundary Gray:** Outline and outline-variant support text fields, low-emphasis boundaries, and disabled or fallback states.

### Named Rules
**The Device Leads Rule.** When Android's dynamic color is available, Material You roles are authoritative. Do not hard-code a brand palette into individual components.

**The Status Is Semantic Rule.** Pending, active, completed, and missed states use their semantic status roles consistently. Status color communicates task truth; it must not be reassigned for decoration.

## Typography

**Display Font:** Roboto (Android system sans-serif)
**Body Font:** Roboto (Android system sans-serif)

**Character:** The incumbent system uses the default Material 3 type scale. It is legible, familiar, and fully responsive to platform text preferences. A custom family may be applied uniformly through the provided typography helper, but role metrics remain Material 3 defaults.

### Hierarchy
- **Display:** Material 3 display roles; reserved for rare, high-impact page moments and large entity titles.
- **Headline:** Material 3 headline roles; used for dashboard greeting and screen-level context.
- **Title:** Material 3 title roles; used for actionable cards, section names, and menu options.
- **Body:** Material 3 body roles; used for dates, metadata, note excerpts, descriptions, and supporting task information.
- **Label:** Material 3 label roles; used for status badges, priority badges, navigation labels, and compact controls.

### Named Rules
**The Role-Not-Size Rule.** Select a Material typography role by semantic purpose, never by ad hoc font-size values in a screen.

## Layout

The app follows Material 3's native Android layout model: top app bars establish screen context; scrolling content uses a 16dp horizontal gutter and a 16dp vertical rhythm; compact touch controls retain a 48dp minimum target. The existing shell's floating navigation pill is 56dp high, with 20dp outer horizontal breathing room and a detached 56dp FAB.

On compact screens, the bottom navigation/floating action pattern is the navigation anchor. Larger layouts must graduate to Material's adaptive navigation rail or navigation suite rather than stretching the phone pattern. Screen content must support edge-to-edge rendering while applying status, navigation, display-cutout, and IME insets so no interactive content is occluded.

## Elevation & Depth

Depth is primarily tonal. Surface container roles create ordered layers before shadow is considered. The floating navigation pill, FAB, and menu cards use moderate elevation to clarify that they are temporarily above content; ordinary cards should not receive arbitrary shadows.

**The Tonal-First Rule.** Establish hierarchy with the Material surface-container ladder. Add shadow only when an element must read as physically floating or temporarily elevated.

## Shapes

The system is gently rounded but not inflated. M3 shape roles map to specific purposes: extra-small for text fields and menus, small for chips and switches, medium for cards and small FABs, large for extended FABs and dialogs, and extra-large for bottom sheets. A 12dp compact-control shape serves dense controls; 50% rounded corners create pills and circular expressive accents.

Rounded polygons and morph adapters exist for expressive clipping and animated transitions. They are a special effect, not the default component silhouette.

**The Soft-Structure Rule.** Use the established M3 radius tier that matches the component's function; do not introduce one-off corner radii.

## Components

### Buttons
- **Character:** Native Material actions with color reserved for a single clear primary action.
- **Primary:** Filled with the Material primary/on-primary pair; the circular 56dp FAB is the canonical creation action.
- **Secondary:** Filled tonal buttons and text buttons carry secondary, contextual, or dialog actions.
- **Feedback:** Pressed and selected states may use the system's medium-bouncy, low-stiffness spring language when motion is enabled.

### Chips
- **Style:** Pills or small M3 containers with semantic color at low alpha.
- **State:** Status and priority badges use label-small typography, 3dp vertical padding, and 6–8dp horizontal padding. Semantic colors explain status; they are not general tags.

### Cards / Containers
- **Corner Style:** Usually medium rounded geometry; menu options may use a 16dp local shape.
- **Background:** Surface container roles create separation; cards may also participate in the optional page gradient.
- **Shadow Strategy:** Tonal depth by default. Moderate elevation is limited to floating navigation, action menus, and other elements genuinely above the page.
- **Internal Padding:** Existing card patterns use 16dp vertical and 20dp horizontal breathing room for menu options; screen lists use a 16dp rhythm.

### Inputs / Fields
- **Style:** Material outlined text fields and standard Material pickers; compact controls use 12dp geometry where a custom shape is needed.
- **Focus:** Material focus and content-color roles remain authoritative; never suppress TalkBack labels or rely on placeholder text as the only label.
- **Error / Disabled:** Use Material error and outline roles, including their dark and contrast-aware scheme variants.

### Navigation
- **Character:** A compact floating navigation pill containing three destinations, paired with a single detached circular FAB.
- **Selected state:** The active destination expands from icon-only to icon plus label, using a primary-container fill and a medium-bouncy spring.
- **Unselected state:** Icon-only, transparent, on-surface-variant.
- **Adaptive behavior:** The floating compact pattern is for phone widths. Use a Material navigation rail or navigation suite on expanded layouts.

### Theme Atmosphere
- **Style:** Optional vertical gradient: a low-alpha primary-container/wallpaper tint resolves into the surface. Transparent top app bars allow this atmosphere to show through.
- **Modes:** System dark/light, optional OLED black, optional enhanced surface tinting, Material You dynamic color, curated seeds, and custom seed color are all supported.
- **Motion:** Respect Android animation scale. Reduced-motion resolves animation to snaps rather than continuing ambient motion.

## Do's and Don'ts

### Do:
- **Do** let Material You/dynamic color set the active palette on Android 12+; use the fallback seed only when necessary.
- **Do** use the Material surface container ladder to separate regions before adding elevation.
- **Do** map every piece of text to a Material typography role and keep it responsive to user font settings.
- **Do** keep touch targets at or above 48dp and apply edge-to-edge insets to bars, content, and IME-sensitive fields.
- **Do** use transparent app bars only where the gradient/surface underneath preserves legibility.
- **Do** disable or simplify motion when the system animation scale is zero.

### Don't:
- **Don't** hard-code raw fallback colors into screen components when `MaterialTheme.colorScheme` provides the semantic role.
- **Don't** treat every card as a floating object; preserve the tonal-first depth hierarchy.
- **Don't** add iOS-shaped controls, bottom-only navigation on expanded screens, or Back behavior that bypasses Android predictive Back.
- **Don't** use status colors to decorate unrelated content or obscure the distinction between completed and missed work.
- **Don't** introduce arbitrary corner radii, typography sizes, shadows, or custom control replicas when a Material component exists.

---
name: Zen Precision
colors:
  surface: '#fbf9f8'
  surface-dim: '#dcd9d9'
  surface-bright: '#fbf9f8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f3'
  surface-container: '#f0eded'
  surface-container-high: '#eae8e7'
  surface-container-highest: '#e4e2e1'
  on-surface: '#1b1c1c'
  on-surface-variant: '#41493e'
  inverse-surface: '#303030'
  inverse-on-surface: '#f2f0f0'
  outline: '#717a6d'
  outline-variant: '#c0c9bb'
  surface-tint: '#2a6b2c'
  primary: '#00450d'
  on-primary: '#ffffff'
  primary-container: '#1b5e20'
  on-primary-container: '#90d689'
  inverse-primary: '#91d78a'
  secondary: '#5d5f5b'
  on-secondary: '#ffffff'
  secondary-container: '#e0e0db'
  on-secondary-container: '#62635f'
  tertiary: '#393b3b'
  on-tertiary: '#ffffff'
  tertiary-container: '#505252'
  on-tertiary-container: '#c5c5c5'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#acf4a4'
  primary-fixed-dim: '#91d78a'
  on-primary-fixed: '#002203'
  on-primary-fixed-variant: '#0c5216'
  secondary-fixed: '#e3e3de'
  secondary-fixed-dim: '#c6c7c2'
  on-secondary-fixed: '#1a1c19'
  on-secondary-fixed-variant: '#454744'
  tertiary-fixed: '#e2e2e2'
  tertiary-fixed-dim: '#c6c6c6'
  on-tertiary-fixed: '#1a1c1c'
  on-tertiary-fixed-variant: '#454747'
  background: '#fbf9f8'
  on-background: '#1b1c1c'
  surface-variant: '#e4e2e1'
typography:
  display-lg:
    fontFamily: Noto Serif
    fontSize: 48px
    fontWeight: '600'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Noto Serif
    fontSize: 32px
    fontWeight: '500'
    lineHeight: '1.2'
  headline-sm:
    fontFamily: Noto Serif
    fontSize: 24px
    fontWeight: '500'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.05em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 8px
  gutter: 24px
  margin-mobile: 20px
  margin-desktop: 64px
  container-max: 1200px
---

## Brand & Style

The design system is anchored in the concept of "Culinary Sanctuary." It evokes the feeling of a high-end, professionally organized kitchen where every tool has a place and every movement is intentional. The brand personality is calm, expert, and sophisticated, targeting home cooks who value the process as much as the result.

The visual style is a rigorous **Minimalism** blended with **Modern Corporate** precision. It prioritizes "negative space as a functional element," ensuring the interface never feels cluttered, even during complex multi-step recipes. The aesthetic avoids unnecessary ornamentation, relying on exquisite typography and a singular, deep accent color to guide the user’s focus. The emotional response is one of confidence and serenity, reducing the "cognitive noise" often associated with busy kitchen environments.

## Colors

The palette is strictly curated to maintain a high-end, gallery-like atmosphere. The foundation is a pure white (#FFFFFF), providing a clean slate that feels airy and hygienic. 

- **Primary:** Deep Forest Green (#1B5E20) is used sparingly for critical actions, active states, and branding moments. It represents growth, freshness, and professional stability.
- **Secondary:** A soft, warm off-white/bone (#F5F5F0) is used for subtle grouping containers to prevent the UI from feeling "clinical."
- **Tertiary:** A light silver-grey (#E0E0E0) defines thin borders and structural dividers.
- **Neutral:** A dark charcoal (#424242) is used for body text to ensure high legibility without the harshness of pure black.

## Typography

This design system utilizes a sophisticated typographic pairing to balance editorial elegance with functional utility.

**Noto Serif** is the voice of the brand, used for recipe titles, section headings, and storytelling elements. Its refined serifs and classic proportions evoke the feel of a premium printed cookbook.

**Inter** provides the functional backbone. It is used for all instructional content, ingredient lists, and interface labels. The high x-height and neutral character ensure maximum legibility at a glance, crucial for users who may be viewing their device from a distance while cooking. Uppercase styling with increased letter spacing is applied to labels and metadata to provide a clear hierarchy against body text.

## Layout & Spacing

The layout philosophy follows a **Fixed Grid** model for large screens to maintain an editorial, "centered" feel, transitioning to a flexible fluid system for mobile devices. 

A strict 8px rhythm governs all spatial relationships. Large margins (64px on desktop) are intentional, creating the "breathing room" required for the Zen aesthetic. In recipe views, a asymmetrical grid is encouraged: a wider column for primary instructions and a narrower side column for ingredients and tools, mimicking professional mise-en-place organization.

## Elevation & Depth

Depth in this design system is achieved through a combination of **low-contrast outlines** and **ambient shadows**. 

Physical layers are minimized to keep the interface flat and modern. When elevation is necessary—such as for a floating timer or a detail card—it uses an extremely diffused, low-opacity shadow (4% to 8% opacity) with no "point" light source, creating a soft glow rather than a harsh drop shadow. 

Thin, 1px lines in light grey (#E0E0E0) are the primary tool for sectioning content. This creates a "blueprint" precision that feels architectural and intentional. Tonal layering is used sparingly, with the secondary off-white color serving as a base for cards or inset areas to distinguish them from the primary white background.

## Shapes

The shape language is characterized by **Soft** precision. A 0.25rem (4px) base radius is used for primary UI elements like buttons and input fields. Larger containers, such as recipe cards or image headers, may use the `rounded-lg` (8px) setting.

This slight rounding prevents the UI from feeling sharp or aggressive while maintaining a structured, professional appearance. It echoes the subtle curves of modern kitchen appliances—functional, ergonomic, and clean. Circular shapes are reserved strictly for status indicators (e.g., "Active Step") or profile avatars to provide a distinct visual break from the rectangular grid.

## Components

The components within the design system prioritize clarity and tactile feedback.

- **Buttons:** Primary buttons are solid Deep Forest Green with white text. Secondary buttons use a 1px green outline with green text. All buttons have a subtle hover transition that slightly deepens the green or adds a very light grey background.
- **Recipe Cards:** These use a white background with a 1px border. Images should be high-quality, top-down or minimally styled photography. Text within cards is left-aligned to maintain a clean vertical axis.
- **Input Fields:** Search and form inputs are represented by a simple 1px bottom border that thickens slightly or changes to the primary green on focus. This mimics the "understated" look of high-end stationary.
- **Chips:** Used for dietary tags (e.g., "Vegan," "Gluten-Free"), these are displayed in a light grey border with uppercase label-md typography.
- **Lists:** Ingredient lists use a custom checkbox that is a simple square outline. When checked, it fills with a light green tint and a thin checkmark, ensuring the item remains legible even when "completed."
- **Timers:** Large, thin-stroke circular indicators that use the primary green for progress. The typography inside is always the sans-serif for instant readability.
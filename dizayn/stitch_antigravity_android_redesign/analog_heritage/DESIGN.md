---
name: Analog Heritage
colors:
  surface: '#fcf9f8'
  surface-dim: '#dcd9d9'
  surface-bright: '#fcf9f8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3f2'
  surface-container: '#f0eded'
  surface-container-high: '#eae7e7'
  surface-container-highest: '#e4e2e1'
  on-surface: '#1b1c1c'
  on-surface-variant: '#45464d'
  inverse-surface: '#303030'
  inverse-on-surface: '#f3f0f0'
  outline: '#76777e'
  outline-variant: '#c6c6cd'
  surface-tint: '#565e77'
  primary: '#040c21'
  on-primary: '#ffffff'
  primary-container: '#1a2238'
  on-primary-container: '#8189a4'
  inverse-primary: '#bec6e3'
  secondary: '#a13c3f'
  on-secondary: '#ffffff'
  secondary-container: '#ff8484'
  on-secondary-container: '#751c22'
  tertiary: '#0c0d0b'
  on-tertiary: '#ffffff'
  tertiary-container: '#222321'
  on-tertiary-container: '#8a8a87'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2ff'
  primary-fixed-dim: '#bec6e3'
  on-primary-fixed: '#131b30'
  on-primary-fixed-variant: '#3e465e'
  secondary-fixed: '#ffdad8'
  secondary-fixed-dim: '#ffb3b1'
  on-secondary-fixed: '#410007'
  on-secondary-fixed-variant: '#82252a'
  tertiary-fixed: '#e4e2de'
  tertiary-fixed-dim: '#c8c6c3'
  on-tertiary-fixed: '#1b1c1a'
  on-tertiary-fixed-variant: '#474744'
  background: '#fcf9f8'
  on-background: '#1b1c1c'
  surface-variant: '#e4e2e1'
typography:
  display-lg:
    fontFamily: Newsreader
    fontSize: 48px
    fontWeight: '600'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Newsreader
    fontSize: 32px
    fontWeight: '500'
    lineHeight: '1.2'
  title-sm:
    fontFamily: Newsreader
    fontSize: 20px
    fontWeight: '600'
    lineHeight: '1.4'
  body-lg:
    fontFamily: Work Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Work Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  data-mono:
    fontFamily: Work Sans
    fontSize: 14px
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: 0.05em
  label-caps:
    fontFamily: Work Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.1em
spacing:
  unit: 8px
  container-max: 1200px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style

This design system establishes a bridge between the timeless authority of a leather-bound cookbook and the precision of modern computational gastronomy. The brand personality is **Sophisticated, Traditional, and Authoritative**, aimed at culinary enthusiasts who value heritage techniques as much as AI-driven precision.

The visual style is **Tactile Minimalism**. It leverages the warmth of physical materials—specifically heavy-weight cream paper and ink—while maintaining the functional clarity of a professional kitchen tool. The emotional response should be one of "Digital Heirloom": a reliable, quiet interface that feels permanent rather than ephemeral.

## Colors

The palette is rooted in the "Ink on Paper" philosophy. The foundation is a warm **Cream Paper (#FDFBF7)** which reduces eye strain and provides a tactile, organic feel. 

- **Primary (Deep Navy):** Used for primary structural elements and high-level navigation to convey reliability.
- **Secondary (Burgundy):** Reserved for meaningful accents, call-to-actions, and highlights that require an editorial touch.
- **Charcoal Text:** All typography is set in a slightly softened charcoal rather than pure black to mimic the appearance of dried ink on a porous surface.
- **Functional Accents:** AI-driven data should use a desaturated variant of the primary navy to distinguish technical insights from culinary content.

## Typography

This design system uses a dual-type system to balance storytelling with technical data.

- **Newsreader (Serif):** Employed for all narrative and editorial content. It features high stroke contrast and traditional serifs, providing the "Heritage" feel. Use optical sizing to ensure elegance at large scales.
- **Work Sans (Sans-Serif):** Used for technical data, ingredient lists, and instructions. Its clean, grounded letterforms ensure legibility in high-pressure cooking environments.
- **Stylistic Note:** Headings should occasionally use italic variants of Newsreader to emphasize "Chef's Notes" or AI insights.

## Layout & Spacing

The layout follows a **Fixed Grid** model on desktop and a fluid model on mobile, mimicking the static beauty of a printed page layout. 

- **Grid:** A 12-column grid is used for recipe discovery, while a single, focused column is used for the "Cooking Mode."
- **Rhythm:** Spacing is generous to allow the "paper" to breathe. Use large vertical margins (48px+) between major recipe sections to maintain an editorial feel.
- **Dividers:** Use 0.5pt or 1pt solid dividers in charcoal with 20% opacity. For specific sections, a "double-rule" (two thin lines close together) signifies a change in chapter or category.

## Elevation & Depth

Depth in this design system is achieved through **Tonal Layering** and physical metaphors rather than shadows.

- **Surface Layers:** Elements do not "float" with shadows; instead, they sit "on top" of the paper. Use subtle shifts in background color (e.g., a slightly darker cream or a very light gray-wash) to define nested containers.
- **Paper Textures:** Apply a subtle grain or paper-fiber overlay at 2-3% opacity across the entire UI.
- **Physical Depth:** Use inset borders (1px) for input fields to make them feel "pressed" into the paper. Avoid large drop shadows; if elevation is required for a modal, use a crisp, 1px charcoal border with a very soft, high-offset ambient blur.

## Shapes

The shape language is **Sharp and Architectural**. 

- **Corners:** Standard buttons, cards, and containers use 0px roundedness (sharp corners) to mimic the edges of paper and traditional printing blocks. 
- **AI Elements:** Only "Data Chips" (AI-generated values like temperature, weight, or time) may use a subtle 2px radius to gently distinguish them from the purely "Analog" content.
- **Icons:** Use hand-drawn, illustrative stroke icons for culinary actions (whisk, flame, knife). Use sharp, geometric icons for technical system actions (save, share, settings).

## Components

- **Buttons:** Primary buttons are solid Charcoal or Navy with white/cream text. Secondary buttons are outlined (1px) with no fill. All buttons are rectangular with no corner radius.
- **AI Data Chips:** Small, high-contrast rectangles with Navy backgrounds and white Work Sans text. These represent the "sharp" AI data injected into the "soft" analog environment.
- **Recipe Cards:** Borderless on the main paper background, separated by generous whitespace and a single thin horizontal divider at the bottom.
- **Input Fields:** Bottom-border only (like a signature line) or a full 1px inset border. No heavy fills.
- **Progress Indicators:** A thin, horizontal line that fills from left to right. Avoid circular loaders; use a simple "Processing..." text in italic Newsreader for an editorial feel.
- **Measurement Toggles:** Use a segmented control with sharp corners to switch between Metric and Imperial, maintaining the appearance of a physical toggle switch.
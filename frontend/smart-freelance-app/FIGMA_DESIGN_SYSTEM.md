# Figma design system reference (Smart Freelance)

Use this when integrating Figma designs via the Figma plugin (e.g. `get_design_context`). Map Figma tokens to the tokens below.

## Design tokens

- **Location:** `src/styles/_variables.scss` (SCSS), `src/styles.scss` (CSS custom properties in `:root`).
- **Colors:** Use CSS vars: `--primary`, `--secondary`, `--accent-gradient`, `--bg-base`, `--bg-card`, `--text-primary`, `--text-secondary`, `--border-light`, `--error`, `--success`, `--shadow-sm/md/lg`.
- **Spacing (8pt grid):** `--spacing-xs` (4px) through `--spacing-3xl` (64px).
- **Typography:** `--text-xs` … `--text-5xl`, `--font-body`, `--font-heading`.
- **Radius:** `--radius-sm` (8px), `--radius-md`, `--radius-lg`, `--radius-xl`.

## Component library

- **Shared:** `src/app/shared/components/` — card, button, badge, sidebar, header, footer.
- **Card:** `app-card` uses `.card`; padding via `.card-md` / `.card-lg`; use `var(--bg-card)`, `var(--shadow-md)`, `var(--radius-lg)`.

## Stack

- **Framework:** Angular (standalone components).
- **Styling:** SCSS + global CSS variables; no Tailwind.
- **Charts:** Chart.js + ng2-charts.

## Figma URL usage

To pull a design into this app, share a Figma URL like:
`https://figma.com/design/<fileKey>/<fileName>?node-id=1-2`

Then use the plugin’s **get_design_context** with the extracted `fileKey` and `nodeId` (e.g. `1:2`) to get code and assets, and adapt the output to Angular + this design system.

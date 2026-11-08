// Material 3 color scheme generated from one seed color (material-color-utilities), light or dark by system setting.
import {
  argbFromHex,
  hexFromArgb,
  Hct,
  SchemeContent,
} from 'https://esm.run/@material/material-color-utilities@0.4.0';

/** Seed of the whole palette: a calm sea-blue for a hotel app. */
const SEED = '#2B6C80';

const ROLES = [
  'primary', 'onPrimary', 'primaryContainer', 'onPrimaryContainer',
  'secondary', 'onSecondary', 'secondaryContainer', 'onSecondaryContainer',
  'tertiary', 'onTertiary', 'tertiaryContainer', 'onTertiaryContainer',
  'error', 'onError', 'errorContainer', 'onErrorContainer',
  'surface', 'onSurface', 'onSurfaceVariant',
  'surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer',
  'surfaceContainerHigh', 'surfaceContainerHighest', 'surfaceDim', 'surfaceBright',
  'outline', 'outlineVariant', 'inverseSurface', 'inverseOnSurface', 'inversePrimary',
];

const toToken = (role) => `--md-sys-color-${role.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)}`;

function applyScheme(isDark) {
  const scheme = new SchemeContent(Hct.fromInt(argbFromHex(SEED)), isDark, 0.0);
  const root = document.documentElement;
  for (const role of ROLES) {
    root.style.setProperty(toToken(role), hexFromArgb(scheme[role]));
  }
}

export function applyTheme() {
  const dark = window.matchMedia('(prefers-color-scheme: dark)');
  applyScheme(dark.matches);
  dark.addEventListener('change', (event) => applyScheme(event.matches));
}

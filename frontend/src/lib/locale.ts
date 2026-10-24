// The backend accepts any ISO 639-1 / ISO 3166-1 code; these are the ones offered in the pickers.
// Display names come from the browser's Intl data, so nothing is hard-coded per language.

export const LANGUAGES = [
  'ko', 'en', 'ja', 'zh', 'es', 'fr', 'de', 'it', 'pt', 'ru',
  'vi', 'th', 'id', 'tl', 'hi', 'ar', 'tr', 'nl', 'sv', 'pl',
]

export const COUNTRIES = [
  'KR', 'US', 'CA', 'GB', 'AU', 'NZ', 'IE', 'JP', 'CN', 'TW', 'HK', 'SG', 'VN', 'TH', 'PH',
  'ID', 'MY', 'IN', 'FR', 'DE', 'ES', 'IT', 'PT', 'NL', 'SE', 'PL', 'RU', 'TR', 'BR', 'MX', 'AR',
]

const languageNames = new Intl.DisplayNames(['en'], { type: 'language' })
const regionNames = new Intl.DisplayNames(['en'], { type: 'region' })

export const languageName = (code: string) => languageNames.of(code) ?? code
export const countryName = (code: string) => regionNames.of(code) ?? code

/** "KR" -> 🇰🇷 via regional indicator symbols. */
export const flag = (country: string) =>
  String.fromCodePoint(...[...country.toUpperCase()].map((c) => 0x1f1a5 + c.charCodeAt(0)))

export function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })
}

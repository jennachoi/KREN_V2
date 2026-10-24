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

const pad = (n: number) => String(n).padStart(2, '0')

/** v1 letter timestamps: 21/08/23 20:39 */
export function letterDate(iso: string) {
  const d = new Date(iso)
  return `${pad(d.getFullYear() % 100)}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** v1 info rows: 2021/08/11 */
export function dayDate(iso: string) {
  const d = new Date(iso)
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
}

export function timeAgo(iso: string) {
  const s = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000)
  if (s < 60) return 'just now'
  if (s < 3600) return `${Math.floor(s / 60)}m`
  if (s < 86400) return `${Math.floor(s / 3600)}h`
  return `${Math.floor(s / 86400)}days`
}

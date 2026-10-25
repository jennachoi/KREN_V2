import { createAvatar } from '@dicebear/core'
import * as avataaars from '@dicebear/avataaars'

/**
 * Illustrated profile pictures, Memoji-style: avatar presets ("m01".."m40") are seeds for DiceBear's
 * "avataaars" (by Pablo Stanley, free for personal and commercial use). Expressions are limited to
 * friendly ones so no random seed ends up crying or grimacing on a penpal profile; see the options
 * below for what else is left out.
 */
const cache = new Map<string, string>()

/** Loaded lazily by ./avatar.ts — the avataaars artwork is ~140 kB, so it stays out of the first bundle. */
export function avatarUri(preset: string) {
  let uri = cache.get(preset)
  if (!uri) {
    uri = createAvatar(avataaars, {
      seed: `kren-${preset}`,
      backgroundColor: ['ffe3e3', 'fff3bf', 'd3f9d8', 'd0ebff', 'e5dbff', 'ffe8cc', 'c5f6fa', 'f3d9fa'],
      backgroundType: ['solid'],
      mouth: ['smile', 'default', 'twinkle'],
      eyes: ['default', 'happy', 'wink', 'squint', 'side'],
      eyebrows: ['default', 'defaultNatural', 'raisedExcited', 'raisedExcitedNatural', 'flatNatural'],
      facialHairProbability: 12,
      accessoriesProbability: 25,
      // Natural skin tones only (no cartoon yellow/orange), no eyepatch, and no slogan T-shirts (a profile picture shouldn't say
      // "RESIST!" on someone's behalf).
      skinColor: ['614335', 'd08b5b', 'ae5d29', 'edb98a', 'ffdbb4'],
      accessories: ['kurt', 'prescription01', 'prescription02', 'round', 'sunglasses', 'wayfarers'],
      clothing: ['blazerAndShirt', 'blazerAndSweater', 'collarAndSweater', 'hoodie', 'overall', 'shirtCrewNeck', 'shirtScoopNeck', 'shirtVNeck'],
    }).toDataUri()
    cache.set(preset, uri)
  }
  return uri
}

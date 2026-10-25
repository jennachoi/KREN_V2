import { useEffect, useState } from 'react'

/**
 * Illustrated profile pictures (presets "m01".."m40"). The renderer and its artwork load on first
 * use; until then avatars show their initial, so the first paint doesn't wait on them.
 */
type Render = (preset: string) => string

let render: Render | null = null
let loading: Promise<Render> | null = null

function loadRenderer() {
  loading ??= import('./avatarRender').then((m) => (render = m.avatarUri))
  return loading
}

export const isPreset = (avatar: string | null | undefined): avatar is string => !!avatar && /^m\d{2}$/.test(avatar)

/** Data URI for a preset, or null while the renderer is still loading. */
export function useAvatarUri(preset: string | null | undefined): string | null {
  const [, setReady] = useState(render !== null)
  useEffect(() => {
    if (render || !isPreset(preset)) return
    let alive = true
    loadRenderer().then(() => alive && setReady(true))
    return () => {
      alive = false
    }
  }, [preset])
  return isPreset(preset) && render ? render(preset) : null
}

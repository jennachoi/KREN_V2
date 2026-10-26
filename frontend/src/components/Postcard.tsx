import type { ReactNode } from 'react'

/**
 * The v1 postcard. From the `sm` breakpoint up it is the original postcard artwork at its fixed
 * proportions; on phones those proportions leave no room to write, so the same content sits on a
 * plain card with a "POST CARD" heading and grows with its content.
 */
export function Postcard({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-[70vh] flex-col rounded-[4px] border border-line bg-white px-5 pt-5 pb-4 shadow-[0_6px_18px_rgba(0,0,0,0.12)] sm:aspect-[863/587] sm:min-h-0 sm:rounded-none sm:border-0 sm:bg-transparent sm:bg-[url(/img/postcard.png)] sm:bg-[length:100%_100%] sm:bg-no-repeat sm:px-[7%] sm:pt-[12%] sm:pb-[6%] sm:shadow-none">
      <div className="mb-4 flex items-start justify-between sm:hidden">
        <span className="mx-auto pl-10 font-postcard text-[13px] tracking-[0.35em] text-ink">POST CARD</span>
        <img src="/img/stamp.png" alt="" className="h-10 w-10 opacity-80" />
      </div>
      {children}
    </div>
  )
}

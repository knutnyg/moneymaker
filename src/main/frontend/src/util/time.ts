export function formatDate(d: Date): string {
  const parts = d.toISOString().split('T')
  const date = parts[0]
  const time = parts[1].substr(0, 8)
  return `${date} ${time}`
}

// in miliseconds
const units = {
  year: 24 * 60 * 60 * 1000 * 365,
  month: (24 * 60 * 60 * 1000 * 365) / 12,
  day: 24 * 60 * 60 * 1000,
  hour: 60 * 60 * 1000,
  minute: 60 * 1000,
  second: 1000,
}

const rtf = new Intl.RelativeTimeFormat('nb', { numeric: 'auto' })

export const getRelativeTime = (d1: Date, d2 = new Date()) => {
  const elapsed = d1.valueOf() - d2.valueOf()

  // "Math.abs" accounts for both "past" & "future" scenarios
  for (var u in units) {
    // @ts-ignore
    if (Math.abs(elapsed) > units[u] || u === 'second') {
      // @ts-ignore
      return rtf.format(Math.round(elapsed / units[u]), u)
    }
  }
}

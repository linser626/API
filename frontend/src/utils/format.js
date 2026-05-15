import dayjs from 'dayjs'

export function formatMoney(amount) {
  if (amount === null || amount === undefined) return '¥0.00'
  const num = Number(amount)
  return '¥' + num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export function formatTokens(tokens) {
  if (tokens === null || tokens === undefined) return '0'
  const num = Number(tokens)
  if (num >= 1000000000) return (num / 1000000000).toFixed(1) + 'B'
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

export function formatDate(date) {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

export function formatPercent(value) {
  if (value === null || value === undefined) return '0%'
  return Number(value).toFixed(1) + '%'
}

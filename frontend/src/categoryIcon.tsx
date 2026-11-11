import {
  AirplaneTakeoff,
  ArrowCounterClockwise,
  ArrowsClockwise,
  Car,
  DeviceMobile,
  Fire,
  FirstAid,
  ForkKnife,
  GameController,
  Gift,
  Handbag,
  Heart,
  House,
  Lightning,
  Martini,
  Money,
  MusicNotes,
  PiggyBank,
  Pizza,
  Popcorn,
  ShieldCheck,
  ShoppingBag,
  Sunglasses,
  Train,
  Wallet,
  WifiHigh,
  type Icon as PhosphorIcon,
} from '@phosphor-icons/react'

const ICONS: Record<string, PhosphorIcon> = {
  AirplaneTakeoff,
  ArrowCounterClockwise,
  ArrowsClockwise,
  Car,
  DeviceMobile,
  Fire,
  FirstAid,
  ForkKnife,
  GameController,
  Gift,
  Handbag,
  Heart,
  House,
  Lightning,
  Martini,
  Money,
  MusicNotes,
  PiggyBank,
  Pizza,
  Popcorn,
  ShieldCheck,
  ShoppingBag,
  Sunglasses,
  Train,
  Wallet,
  WifiHigh,
}

const ICON_NAMES = new Map<PhosphorIcon, string>(Object.entries(ICONS).map(([name, icon]) => [icon, name]))

export const PICKER_ICONS: PhosphorIcon[] = [
  ForkKnife,
  Pizza,
  Popcorn,
  GameController,
  MusicNotes,
  ShoppingBag,
  Handbag,
  Sunglasses,
  Car,
  Train,
  AirplaneTakeoff,
  FirstAid,
  Heart,
  House,
  Lightning,
  Gift,
  Fire,
  Money,
  PiggyBank,
  Wallet,
]

const RULES: Array<[RegExp, string]> = [
  [/mietwagen/, 'Car'],
  [/ess(e|en)|restaurant|caf(e|é)|b(a|ä)ckerei|supermarkt|lebensmittel|rewe|edeka|aldi|lidl/, 'ForkKnife'],
  [/freizeit|kino|spiel/, 'GameController'],
  [/konzert|musik/, 'MusicNotes'],
  [/miete|wohnung|haus|hypothek|nebenkosten/, 'House'],
  [/strom|energie/, 'Lightning'],
  [/internet|wifi|netzwerk/, 'WifiHigh'],
  [/telefon|handy/, 'DeviceMobile'],
  [/versicher/, 'ShieldCheck'],
  [/abo|netflix|spotify/, 'ArrowsClockwise'],
  [/gehalt|lohn|salary/, 'Money'],
  [/erstattung|refund|gutschrift/, 'ArrowCounterClockwise'],
  [/urlaub|reise|flug|hotel/, 'AirplaneTakeoff'],
  [/bahn/, 'Train'],
  [/transport|auto|bus|tank/, 'Car'],
  [/shopping|kleidung|einkauf/, 'ShoppingBag'],
  [/gesundheit|arzt|apotheke|medizin/, 'FirstAid'],
  [/sport|fitness/, 'Heart'],
  [/geschenk/, 'Gift'],
  [/spar/, 'PiggyBank'],
]

const STORAGE_KEY = 'lunefi:kategorieIcons'
let overrides: Record<string, string> | null = null

function readOverrides(): Record<string, string> {
  if (overrides === null) {
    try {
      overrides = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}') as Record<string, string>
    } catch {
      overrides = {}
    }
  }
  return overrides
}

export function setCategoryIcon(name: string, phosphorName: string) {
  overrides = { ...readOverrides(), [name.trim().toLowerCase()]: phosphorName }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(overrides))
}

export function categoryIconName(name: string): string {
  const klein = name.trim().toLowerCase()
  const override = readOverrides()[klein]
  if (override && ICONS[override]) return override
  for (const [muster, icon] of RULES) {
    if (muster.test(klein)) return icon
  }
  return 'Wallet'
}

export function iconNameFrom(icon: PhosphorIcon): string {
  return ICON_NAMES.get(icon) ?? 'Wallet'
}

export function categoryIcon(name: string): PhosphorIcon {
  return ICONS[categoryIconName(name)] ?? Wallet
}

export function iconFromName(name: string): PhosphorIcon {
  return ICONS[name] ?? Wallet
}

export function CategoryIcon({ name, size = 17, className }: { name: string; size?: number; className?: string }) {
  const Icon = categoryIcon(name)
  return (
    <Icon
      size={size}
      weight="regular"
      className={`shrink-0 ${className ?? 'text-textschwach'}`}
      aria-hidden="true"
    />
  )
}

export function CategoryIconTile({
  name,
  groesse = 28,
  iconGroesse = 16,
  iconKlasse,
}: {
  name: string
  groesse?: number
  iconGroesse?: number
  iconKlasse?: string
}) {
  return (
    <span
      aria-hidden="true"
      className="flex shrink-0 items-center justify-center rounded-lg"
      style={{
        width: groesse,
        height: groesse,
        background: 'color-mix(in srgb, var(--color-text) 8%, transparent)',
      }}
    >
      <CategoryIcon name={name} size={iconGroesse} className={iconKlasse} />
    </span>
  )
}

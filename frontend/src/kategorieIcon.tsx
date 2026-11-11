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

const NAMEN = new Map<PhosphorIcon, string>(Object.entries(ICONS).map(([name, icon]) => [icon, name]))

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

const REGELN: Array<[RegExp, string]> = [
  [/mietwagen/, 'Car'],
  [/ess(e|en)|restaurant|supermarkt|grocer|lebensmittel/, 'ForkKnife'],
  [/freizeit|kino|spiel/, 'GameController'],
  [/miete|wohnung|haus/, 'House'],
  [/strom|energie/, 'Lightning'],
  [/internet|wifi|netzwerk/, 'WifiHigh'],
  [/telefon|handy/, 'DeviceMobile'],
  [/versicher/, 'ShieldCheck'],
  [/abo/, 'ArrowsClockwise'],
  [/gehalt|lohn|salary/, 'Money'],
  [/erstattung/, 'ArrowCounterClockwise'],
  [/urlaub|reise|flug/, 'AirplaneTakeoff'],
  [/transport|bahn|auto|tanken/, 'Car'],
  [/shopping|kleidung/, 'ShoppingBag'],
  [/gesundheit|arzt|apotheke/, 'FirstAid'],
  [/geschenk/, 'Gift'],
  [/spar/, 'PiggyBank'],
]

const SPEICHERORT = 'lunefi:kategorieIcons'
let overrides: Record<string, string> | null = null

function leseOverrides(): Record<string, string> {
  if (overrides === null) {
    try {
      overrides = JSON.parse(localStorage.getItem(SPEICHERORT) ?? '{}') as Record<string, string>
    } catch {
      overrides = {}
    }
  }
  return overrides
}

export function setzeKategorieIcon(name: string, phosphorName: string) {
  overrides = { ...leseOverrides(), [name.trim().toLowerCase()]: phosphorName }
  localStorage.setItem(SPEICHERORT, JSON.stringify(overrides))
}

export function kategorieIconName(name: string): string {
  const klein = name.trim().toLowerCase()
  const override = leseOverrides()[klein]
  if (override && ICONS[override]) return override
  for (const [muster, icon] of REGELN) {
    if (muster.test(klein)) return icon
  }
  return 'Wallet'
}

export function iconNameVon(icon: PhosphorIcon): string {
  return NAMEN.get(icon) ?? 'Wallet'
}

export function kategorieIcon(name: string): PhosphorIcon {
  return ICONS[kategorieIconName(name)] ?? Wallet
}

export function KategorieIcon({ name, size = 17, className }: { name: string; size?: number; className?: string }) {
  const Icon = kategorieIcon(name)
  return (
    <Icon
      size={size}
      weight="regular"
      className={`shrink-0 ${className ?? 'text-textschwach'}`}
      aria-hidden="true"
    />
  )
}

export function KategorieIconTile({
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
      <KategorieIcon name={name} size={iconGroesse} className={iconKlasse} />
    </span>
  )
}

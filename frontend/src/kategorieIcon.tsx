import {
  AirplaneTakeoff,
  Car,
  FirstAid,
  ForkKnife,
  GameController,
  House,
  Money,
  PiggyBank,
  ShoppingBag,
  Wallet,
  type Icon as PhosphorIcon,
} from '@phosphor-icons/react'

const REGELN: Array<[RegExp, PhosphorIcon]> = [
  [/ess(e|en)|restaurant|supermarkt|grocer|lebensmittel/, ForkKnife],
  [/freizeit|kino|spiel/, GameController],
  [/miete|wohnung|haus/, House],
  [/gehalt|lohn|salary/, Money],
  [/urlaub|reise|flug/, AirplaneTakeoff],
  [/transport|bahn|auto|tanken/, Car],
  [/shopping|kleidung/, ShoppingBag],
  [/gesundheit|arzt|apotheke/, FirstAid],
  [/spar/, PiggyBank],
]

export function kategorieIcon(name: string): PhosphorIcon {
  const klein = name.toLowerCase()
  for (const [muster, icon] of REGELN) {
    if (muster.test(klein)) return icon
  }
  return Wallet
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

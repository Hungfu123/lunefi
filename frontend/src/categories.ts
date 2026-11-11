import {
  AirplaneTakeoff,
  ArrowCounterClockwise,
  ArrowsClockwise,
  Car,
  DeviceMobile,
  FirstAid,
  ForkKnife,
  GameController,
  Gift,
  House,
  Lightning,
  Money,
  ShieldCheck,
  ShoppingBag,
  WifiHigh,
  type Icon as PhosphorIcon,
} from '@phosphor-icons/react'

export type CategoryType = 'fix' | 'variabel' | 'einnahme'

export interface PredefinedCategory {
  name: string
  icon: PhosphorIcon
  typ: CategoryType
}

export const PREDEFINED_CATEGORIES: PredefinedCategory[] = [
  { name: 'Miete', icon: House, typ: 'fix' },
  { name: 'Strom', icon: Lightning, typ: 'fix' },
  { name: 'Internet', icon: WifiHigh, typ: 'fix' },
  { name: 'Telefon', icon: DeviceMobile, typ: 'fix' },
  { name: 'Versicherung', icon: ShieldCheck, typ: 'fix' },
  { name: 'Abo', icon: ArrowsClockwise, typ: 'fix' },
  { name: 'Essen', icon: ForkKnife, typ: 'variabel' },
  { name: 'Freizeit', icon: GameController, typ: 'variabel' },
  { name: 'Shopping', icon: ShoppingBag, typ: 'variabel' },
  { name: 'Transport', icon: Car, typ: 'variabel' },
  { name: 'Gesundheit', icon: FirstAid, typ: 'variabel' },
  { name: 'Reisen', icon: AirplaneTakeoff, typ: 'variabel' },
  { name: 'Geschenke', icon: Gift, typ: 'variabel' },
  { name: 'Gehalt', icon: Money, typ: 'einnahme' },
  { name: 'Erstattung', icon: ArrowCounterClockwise, typ: 'einnahme' },
]

export const TYPE_LABEL: Record<CategoryType, string> = {
  fix: 'fixe Kosten',
  variabel: 'variable Kosten',
  einnahme: 'Einnahme',
}

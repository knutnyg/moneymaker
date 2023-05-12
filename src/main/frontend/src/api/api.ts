export function parseAppState(data: string): AppState {
  const nextState = JSON.parse(data)
  nextState.lastUpdatedAt = new Date(nextState.lastUpdatedAt * 1000)
  nextState.accountBalance = {
    ...nextState.accountBalance,
    lastUpdatedAt: new Date(nextState.accountBalance.lastUpdatedAt * 1000),
  }
  nextState.activeTrades = {
    ...nextState.activeTrades,
    lastUpdatedAt: new Date(nextState.activeTrades.lastUpdatedAt * 1000),
  }
  nextState.activeTrades.activeOrders = nextState.activeTrades.activeOrders.map(
    (o: { created_at: number }) => {
      return {
        ...o,
        created_at: new Date(o.created_at * 1000),
      }
    },
  )
  nextState.filledOrders.filledOrders = nextState.filledOrders.filledOrders.map(
    (o: { created_at: number }) => {
      return {
        ...o,
        created_at: new Date(o.created_at * 1000),
      }
    },
  )
  nextState.filledOrders = {
    ...nextState.filledOrders,
    lastUpdatedAt: new Date(nextState.filledOrders.lastUpdatedAt * 1000),
  }
  nextState.prevActionSet = {
    ...nextState.prevActionSet,
    lastUpdatedAt: new Date(nextState.prevActionSet.lastUpdatedAt * 1000),
  }
  console.log('state=', nextState)
  return nextState
}

export interface ActiveOrder {
  id: number
  market: 'BTCNOK'
  type: 'bid' | 'ask'
  price: number
  remaining: number
  amount: number
  matched: number
  cancelled: number
  created_at: Date
}

export interface ActiveTrades {
  activeOrders: Array<ActiveOrder>
  lastUpdatedAt: Date
}

export type ClearOrdersAction = {
  type: 'ClearOrders'
}
export type AddBidAction = {
  type: 'AddBid'
}
export type AddAskAction = {
  type: 'AddAsk'
}
export type KeepAskAction = {
  type: 'KeepAsk'
}
export type KeepBidAction = {
  type: 'KeepBid'
}

export type Action =
  | ClearOrdersAction
  | AddBidAction
  | AddAskAction
  | KeepAskAction
  | KeepBidAction

export interface ActionsState {
  actions: Array<Action>
  lastUpdatedAt: Date
}

export function displayAction(action: Action): string {
  switch (action.type) {
    case 'ClearOrders':
      return 'Clear orders'
    case 'AddBid':
      return 'Add bid'
    case 'AddAsk':
      return 'Add ask'
    case 'KeepAsk':
      return 'Keep ask'
    case 'KeepBid':
      return 'Keep bid'
  }
}

export interface FilledOrdersState {
  filledOrders: Array<ActiveOrder>
  lastUpdatedAt: Date
}

export interface MarketTicker {
  bid: number
  ask: number
  spread: number
}

export interface MarketState {
  markets: {
    [keyof: string]: MarketTicker
  }
  lastUpdatedAt: Date
}

export interface CurrencyBalance {
  currency: string
  balance: number
  hold: number
  available: number
}

export interface AccountState {
  currencies: Record<Currency, CurrencyBalance>
}

export interface AccountBalanceState {
  account: AccountState
  lastUpdatedAt: Date
}

export interface AppState {
  accountBalance: AccountBalanceState
  market: MarketState
  activeTrades: ActiveTrades
  filledOrders: FilledOrdersState
  prevActionSet: ActionsState
  lastUpdatedAt: Date
}

export enum Currency {
  ADA = 'ADA',
  AVAX = 'AVAX',
  BNB = 'BNB',
  BTC = 'BTC',
  DAI = 'DAI',
  DOT = 'DOT',
  ETH = 'ETH',
  LTC = 'LTC',
  NOK = 'NOK',
  DKK = 'DKK',
  SOL = 'SOL',
  USDC = 'USDC',
  XRP = 'XRP',
}

export enum Markets {
  BTCNOK = 'BTCNOK',
  ETHNOK = 'ETHNOK',
}

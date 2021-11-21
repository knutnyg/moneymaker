
export function parseAppState(data: string): AppState {
    const nextState = JSON.parse(data);
    nextState.lastUpdatedAt = new Date(nextState.lastUpdatedAt * 1000)
    nextState.activeTrades = {
        ...nextState.activeTrades,
        lastUpdatedAt: new Date(nextState.activeTrades.lastUpdatedAt * 1000),
    }
    nextState.activeTrades.activeOrders = nextState.activeTrades.activeOrders.map((o: { created_at: number; }) => {
        return {
            ...o,
            created_at: new Date(o.created_at * 1000),
        }
    })
    nextState.filledOrders.filledOrders = nextState.filledOrders.filledOrders.map((o: { created_at: number; }) => {
        return {
            ...o,
            created_at: new Date(o.created_at * 1000),
        }
    })
    nextState.filledOrders = {
        ...nextState.filledOrders,
        lastUpdatedAt: new Date(nextState.filledOrders.lastUpdatedAt * 1000),
    }
    console.log('state=', nextState);
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

export interface AppState {
    market: MarketState
    activeTrades: ActiveTrades
    filledOrders: FilledOrdersState
    lastUpdatedAt: Date
}

export enum Markets {
    BTCNOK = 'BTCNOK'
}


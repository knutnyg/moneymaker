import './App.scss';
import {Fragment, useEffect, useState} from "react";
import useInterval from "./hooks/useInterval";

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

const RelativeTime: React.FC<{ ts: Date }> = ({ts}) => {
    const [time, setTime] = useState(new Date())
    useInterval(() => {
        setTime(new Date())
    }, 1000)
    return (<span>{getRelativeTime(ts, time)}</span>)
}

const H1: React.FC = ({children}) => <h1 style={{paddingTop: '10px'}}>{children}</h1>
const H2: React.FC = ({children}) => <h2 style={{paddingTop: '10px'}}>{children}</h2>

function AppStateView(props: { state: AppState | undefined }) {
    const state = props.state;

    if (!state) {
        return (<div>Loading...</div>)
    }
    const marketMap = state.market.markets || {};
    const markets = Object.keys(marketMap)
        .map(marketId => ({
            ...marketMap[marketId],
            id: marketId,
        }))

    return (
        <div>
            <div>
                <div>Last updated:</div>
                <div><RelativeTime ts={state.lastUpdatedAt}/></div>
            </div>
            <H2>Market</H2>
            <div style={{
                display: 'grid',
                gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr',
            }}>
                <div style={{fontWeight: 'bold'}}/>
                <div style={{fontWeight: 'bold'}}>Ask</div>
                <div style={{fontWeight: 'bold'}}>Bid</div>
                <div style={{fontWeight: 'bold'}}>Spread</div>
                <div style={{fontWeight: 'bold'}}>%</div>
                {
                    markets.map(m => {
                        const p = m.spread / ((m.ask + m.bid) / 2) * 100
                        return (
                            <Fragment key={m.id}>
                                <div>{m.id}</div>
                                <div>{m.ask.toFixed(0)}</div>
                                <div>{m.bid.toFixed(0)}</div>
                                <div>{m.spread}</div>
                                <div>{p.toFixed(2)}%</div>
                            </Fragment>
                        );
                    })
                }
            </div>
            <H2>Active orders</H2>
            <div>
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: '2fr 1fr 1fr 1fr 2fr 1fr',
                    }}
                >
                    {state.activeTrades.activeOrders.map(a => <Fragment key={`${a.id}`}>
                        <div>{format(a.created_at)}</div>
                        <div>{a.market}</div>
                        <div>{a.type}</div>
                        <div>{a.amount}</div>
                        <div>{a.price.toFixed(2)}</div>
                        <div>{(a.price * a.amount).toFixed(2)} kr</div>
                    </Fragment>)}
                </div>
            </div>
            <H2>Trade History</H2>
            <div>
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: '2fr 1fr 1fr 1fr 2fr 1fr',
                    }}
                >
                    {state.filledOrders.filledOrders.map(a => <Fragment key={`${a.id}`}>
                        <div>{format(a.created_at)}</div>
                        <div>{a.market}</div>
                        <div>{a.type}</div>
                        <div>{a.amount}</div>
                        <div>{a.price.toFixed(2)}</div>
                        <div>{(a.price * a.amount).toFixed(2)} kr</div>
                    </Fragment>)}
                </div>
            </div>
        </div>
    );
}

function format(d: Date): string {
    const parts = d.toISOString().split('T');
    const date = parts[0]
    const time = parts[1].substr(0, 8)
    return `${date} ${time}`
}

function parseMessage(data: string): AppState {
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

function createWebSocket(path: string): string {
    const loc = window.location
    const protocolPrefix = (loc.protocol === 'https:') ? 'wss:' : 'ws:';
    return protocolPrefix + '//' + loc.host + path;
}

const DataSource: React.FC = () => {
    const [error, setError] = useState<string | undefined>()
    const [appState, setAppState] = useState<AppState | undefined>()

    useEffect(() => {

        const url = createWebSocket('/api/app/state/ws')
        console.log('subscribe url=' + url);
        //const wsSource = new WebSocket('ws://localhost:8020/api/app/state/ws');
        const wsSource = new WebSocket(url);
        wsSource.onerror = (err) => {
            console.log('onerror', err)
            setError(`error: ${err}`)
        }
        wsSource.onmessage = (evt) => {
            const jsonData = evt.data;
            try {
                const nextState = parseMessage(jsonData)
                setAppState(nextState)
            } catch (err) {
                console.log('error parsing msg: ', err)
                console.log('error parsing msg data=', jsonData)
            }
        }

        wsSource.onclose = (evt) => {
            console.log('onclose', evt)
            const ts = new Date(evt.timeStamp)
            setError(`connection closed at: ${ts.toISOString()}`)
        }

        return () => {
            console.log('unsubscribe')
            wsSource.close();
        }
    }, [setAppState])

    return (
        <Fragment>
            <div className="main">
                <div className="content">
                    <h1>Moneymaker 🤑</h1>
                    <div>
                        {error && <span>{error}</span>}
                    </div>
                    <div>
                        <AppStateView state={appState}/>
                    </div>
                </div>
            </div>
        </Fragment>
    );
}

function App() {
    return (
        <DataSource>
        </DataSource>
    );
}

export default App;

// in miliseconds
const units = {
    year: 24 * 60 * 60 * 1000 * 365,
    month: 24 * 60 * 60 * 1000 * 365 / 12,
    day: 24 * 60 * 60 * 1000,
    hour: 60 * 60 * 1000,
    minute: 60 * 1000,
    second: 1000
}

const rtf = new Intl.RelativeTimeFormat('nb', {numeric: 'auto'})

const getRelativeTime = (d1: Date, d2 = new Date()) => {
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


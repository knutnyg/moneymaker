import './App.scss';
import React, {Fragment, useEffect, useState} from "react";
import useInterval from "./hooks/useInterval";
import {AppState, displayAction, parseAppState} from "./api/api";
import {formatDate, getRelativeTime} from "./util/time";

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
                        <div>{formatDate(a.created_at)}</div>
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
                        <div>{formatDate(a.created_at)}</div>
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

function createWebSocket(path: string): string {
    const loc = window.location
    const protocolPrefix = (loc.protocol === 'https:') ? 'wss:' : 'ws:';
    return protocolPrefix + '//' + loc.host + path;
}

const Sidebar: React.FC<{ appState?: AppState }> = ({appState}) => {
    if (!appState) {
        return null
    }
    const actions = appState.prevActionSet.actions
    return (
        <aside style={{display: 'grid', justifyContent: 'center', alignContent: 'center'}}>
            <div style={{maxWidth: '280px', width: '280px'}}>
                <div>Latest Actions</div>
                <ol>
                    {actions.map((val, idx) =>
                        <li key={idx}>{displayAction(val)}</li>
                    )}
                </ol>
                <RelativeTime ts={appState.prevActionSet.lastUpdatedAt}/>
            </div>
        </aside>
    );
}

const DataSource: React.FC = () => {
    const [error, setError] = useState<string | undefined>()
    const [appState, setAppState] = useState<AppState | undefined>()

    useEffect(() => {
        const url = createWebSocket('/api/app/state/ws')
        console.log('subscribe to url=' + url);
        const wsSource = new WebSocket(url);
        //const wsSource = new WebSocket('ws://localhost:8020/api/app/state/ws');

        wsSource.onerror = (err) => {
            console.log('onerror', err)
            const ts = new Date(err.timeStamp)
            setError(`error at ${ts.toISOString()}: ${err}`)
        }
        wsSource.onmessage = (evt) => {
            const jsonData = evt.data;
            try {
                const nextState = parseAppState(jsonData)
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
                <div className="layout">
                    <Sidebar appState={appState}/>
                    <div className="content">
                        <h1>Moneymaker 🤑</h1>
                        <div>
                            {error && <span>{error}</span>}
                        </div>
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

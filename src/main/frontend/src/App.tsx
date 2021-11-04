import './App.scss';
import {Fragment, useEffect, useState} from "react";

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
}

export interface AppState {
    activeTrades: ActiveTrades
    lastUpdatedAt: Date
}

function AppStateView(props: { state: AppState | undefined }) {
    const state = props.state;

    if (!state) {
        return (<div>Loading...</div>)
    }
    return (
        <div>
            <div>
                <div>Last updated:</div>
                <div>{state.lastUpdatedAt.toISOString()}</div>
            </div>
            <div>
                <div>{state.activeTrades.activeOrders.map(a => <div>{JSON.stringify(a)}</div>)}</div>
            </div>
        </div>
    );
}

const DataSource: React.FC = () => {
    const [appState, setAppState] = useState<AppState | undefined>()

    useEffect(() => {
        //const source = new EventSource('/api/app/state/listen');
        const source = new EventSource('//localhost:8020/api/app/state/listen');
        source.onmessage = (evt) => {
            console.log('evt=', evt);
            const jsonData = evt.data;
            try {
                const nextState = JSON.parse(jsonData);
                nextState.lastUpdatedAt = new Date(nextState.lastUpdatedAt * 1000)
                nextState.activeTrades.activeOrders = nextState.activeTrades.activeOrders.map((o: { created_at: number; }) => {
                    return {
                        ...o,
                        created_at: new Date(o.created_at * 1000),
                    }
                })
                console.log('state=', nextState);
                setAppState(nextState)
            } catch (e) {
                console.log('err=', e);
            }
        }

        return () => {
            source.close();
        }
    }, [setAppState])

    return (
        <Fragment>
            <div className="main">
                <div className="content">
                    <h1>My app</h1>
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

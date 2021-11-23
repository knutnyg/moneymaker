import './App.scss';
import React, {Fragment, useEffect, useState} from 'react';
import CssBaseline from '@mui/material/CssBaseline';
import useInterval from './hooks/useInterval';
import {AppState, displayAction, parseAppState} from './api/api';
import {formatDate, getRelativeTime} from './util/time';
import {Box, Container, createTheme, Grid, ThemeProvider} from "@mui/material";

const darkTheme = createTheme({
    palette: {
        mode: 'dark',
    },
});

const RelativeTime: React.FC<{ ts: Date }> = ({ts}) => {
    const [time, setTime] = useState(new Date())
    useInterval(() => {
        setTime(new Date())
    }, 1000)
    return (<span>{getRelativeTime(ts, time)}</span>)
}

const H1: React.FC = ({children}) => <h1 style={{paddingTop: '10px'}}>{children}</h1>
const H2: React.FC = ({children}) => <h2 style={{paddingTop: '10px'}}>{children}</h2>

const PrevActionSet: React.FC<{ appState?: AppState, header?: React.ReactElement }> = ({header, appState}) => {
    if (!appState) {
        return null;
    }

    const actions = appState.prevActionSet.actions
    return (
        <Fragment>
            {
                header
                    ? header
                    : <div>Latest Actions</div>
            }
            <ol>
                {actions.map((val, idx) =>
                    <li key={idx}>{displayAction(val)}</li>
                )}
            </ol>
            <RelativeTime ts={appState.prevActionSet.lastUpdatedAt}/>
        </Fragment>
    );
}

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
        <Box>
            <div>
                <div>Last updated:</div>
                <div><RelativeTime ts={state.lastUpdatedAt}/></div>
            </div>
            <H2>Market</H2>
            <div style={{
                display: 'grid',
                gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr',
            }}>
                <Box sx={{
                    display: 'contents',
                }}>
                    <div style={{fontWeight: 'bold'}}/>
                    <div style={{fontWeight: 'bold'}}>Ask</div>
                    <div style={{fontWeight: 'bold'}}>Bid</div>
                    <div style={{fontWeight: 'bold'}}>Spread</div>
                    <div style={{fontWeight: 'bold'}}>%</div>
                </Box>
                {
                    markets.map(m => {
                        const p = m.spread / ((m.ask + m.bid) / 2) * 100
                        return (
                            <Fragment key={m.id}>
                                <Box sx={{
                                    display: 'contents',
                                    '&:hover > div': {backgroundColor: '#333'},
                                    '&:focus > div': {backgroundColor: '#333'},
                                }}>
                                    <div>{m.id}</div>
                                    <div>{m.ask.toFixed(0)}</div>
                                    <div>{m.bid.toFixed(0)}</div>
                                    <div>{m.spread}</div>
                                    <div>{p.toFixed(2)}%</div>
                                </Box>
                            </Fragment>
                        );
                    })
                }
            </div>
            <Box sx={{
                display: ['grid', 'grid', 'none'],
                justifyItems: ['center', 'center', 'unset']
            }}>
                <PrevActionSet appState={state} header={<H2>Latest Actions</H2>}/>
            </Box>
            <H2>Active orders</H2>
            <div>
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: 'minmax(155px, 2fr) 1fr 1fr 1fr 2fr 1fr',
                        gridRowGap: '4px',
                    }}
                >
                    {state.activeTrades.activeOrders.map(a =>
                        <Fragment key={`${a.id}`}>
                            <Box sx={{
                                display: 'contents',
                                '&:hover > div': {backgroundColor: '#333'},
                                '&:focus > div': {backgroundColor: '#333'},
                            }}>
                                <Box>{formatDate(a.created_at)}</Box>
                                <Box sx={{textAlign: 'center'}}>{a.market}</Box>
                                <Box sx={{textAlign: 'center'}}>{a.type}</Box>
                                <Box sx={{textAlign: 'right'}}>{a.amount}</Box>
                                <Box sx={{textAlign: 'right'}}>{a.price.toFixed(2)}</Box>
                                <Box sx={{textAlign: 'right'}}>{(a.price * a.amount).toFixed(2)} kr</Box>
                            </Box>
                        </Fragment>)}
                </div>
            </div>
            <H2>Trade History</H2>
            <div>
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: 'minmax(155px, 2fr) 1fr 1fr 1fr 2fr 1fr',
                        gridRowGap: '4px',
                    }}
                >
                    {state.filledOrders.filledOrders.map(a =>
                        <Fragment key={`${a.id}`}>
                            <Box sx={{
                                display: 'contents',
                                '&:hover > div': {backgroundColor: '#333'},
                                '&:focus > div': {backgroundColor: '#333'},
                            }}>
                                <Box>{formatDate(a.created_at)}</Box>
                                <Box sx={{textAlign: 'center'}}>{a.market}</Box>
                                <Box sx={{textAlign: 'center'}}>{a.type}</Box>
                                <Box sx={{textAlign: 'right'}}>{a.amount}</Box>
                                <Box sx={{textAlign: 'right'}}>{a.price.toFixed(2)}</Box>
                                <Box sx={{textAlign: 'right'}}>{(a.price * a.amount).toFixed(2)} kr</Box>
                            </Box>
                        </Fragment>
                    )}
                </div>
            </div>
        </Box>
    );
}

function createWebSocket(path: string): string {
    const loc = window.location
    const protocolPrefix = (loc.protocol === 'https:') ? 'wss:' : 'ws:';
    return protocolPrefix + '//' + loc.host + path;
}

const Sidebar: React.FC<{ appState?: AppState }> = ({appState}) => {
    return (
        <Box sx={{
            justifyContent: 'center',
            alignContent: 'center',
            maxWidth: ['100%', '280px'],
            width: ['100%', '280px'],
        }}>
            <aside>
                <PrevActionSet appState={appState}/>
            </aside>
        </Box>
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
        <Box sx={{
            display: ['flex', 'flex', 'grid'],
            flexDirection: ['column', 'column', 'unset'],
            gridTemplateColumns: ['unset', 'unset', '1fr 280px'],
            gridTemplateRows: ['unset', 'unset', 'auto'],
            gridTemplateAreas: ['unset', 'unset', '"content sidebar"'],
        }}>
            <Container sx={{
                gridArea: ['unset', 'unset', 'content'],
            }}>
                <Container sx={{
                    display: 'grid',
                    justifyItems: 'center',
                }}>
                    <h1>Moneymaker 🤑</h1>
                </Container>
                <Container sx={{gridArea: ['unset', 'unset', 'content']}}>
                    {error && <span>{error}</span>}
                </Container>
                <Container sx={{
                    gridArea: ['unset', 'unset', 'content'],
                }}>
                    <AppStateView state={appState}/>
                </Container>
            </Container>
            <Container sx={{
                gridArea: ['unset', 'unset', 'sidebar'],
                display: ['none', 'none', 'grid'],
                justifyItems: ['unset', 'unset', 'center'],
                alignItems: ['unset', 'unset', 'center'],
            }}>
                <Sidebar appState={appState}/>
            </Container>
        </Box>
    );
}

function App() {
    return (
        <ThemeProvider theme={darkTheme}>
            <Fragment>
                <CssBaseline/>
                <DataSource/>
            </Fragment>
        </ThemeProvider>
    );
}

export default App;

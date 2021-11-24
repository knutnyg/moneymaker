import React, { Fragment } from "react";
import { AppState, displayAction } from "../api/api";
import { RelativeTime } from "./RelativeTime";

export const PrevActionSet: React.FC<{
  appState?: AppState;
  header?: React.ReactElement;
}> = ({ header, appState }) => {
  if (!appState) {
    return null;
  }

  const actions = appState.prevActionSet.actions;
  return (
    <Fragment>
      {header ? header : <div>Latest Actions</div>}
      <ol>
        {actions.map((val, idx) => (
          <li key={idx}>{displayAction(val)}</li>
        ))}
      </ol>
      <RelativeTime ts={appState.prevActionSet.lastUpdatedAt} />
    </Fragment>
  );
};

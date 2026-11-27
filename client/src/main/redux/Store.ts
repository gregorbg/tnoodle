import { configureStore } from "@reduxjs/toolkit";
import { initialState, scramblerSlice } from "./ScramblerSlice.ts";
import { getQueryParameter } from "../util/query.param.util.ts";
import { isUsingStaging } from "../api/wca.oidc.ts";

export const createStore = () =>
    configureStore({
        reducer: {
            scramblerSlice: scramblerSlice.reducer,
        },
        preloadedState: {
            scramblerSlice: {
                ...initialState,
                competitionId: getQueryParameter("competitionId") ?? undefined,
                isUsingStaging: isUsingStaging(),
            },
        },
    });

export type AppStore = ReturnType<typeof createStore>;

const store = createStore();

export default store;

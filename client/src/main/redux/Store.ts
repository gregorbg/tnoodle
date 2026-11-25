import { configureStore } from "@reduxjs/toolkit";
import { initialState as initialScramblingState, scramblingSlice } from "./slice/ScramblingSlice";
import { wcifSlice } from "./slice/WcifSlice";
import { settingsSlice } from "./slice/SettingsSlice";
import { getQueryParameter } from "../util/query.param.util.ts";

export const createStore = () =>
    configureStore({
        reducer: {
            scramblingSlice: scramblingSlice.reducer,
            wcifSlice: wcifSlice.reducer,
            settingsSlice: settingsSlice.reducer,
        },
        preloadedState: {
            scramblingSlice: {
                ...initialScramblingState,
                competitionId: getQueryParameter("competitionId") ?? undefined,
            }
        }
    });

export type AppStore = ReturnType<typeof createStore>;

const store = createStore();

export default store;

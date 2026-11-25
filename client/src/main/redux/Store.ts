import { configureStore } from "@reduxjs/toolkit";
import { scramblingSlice } from "./slice/ScramblingSlice";
import { wcifSlice } from "./slice/WcifSlice";
import { settingsSlice } from "./slice/SettingsSlice";

export const createStore = () =>
    configureStore({
        reducer: {
            scramblingSlice: scramblingSlice.reducer,
            wcifSlice: wcifSlice.reducer,
            settingsSlice: settingsSlice.reducer,
        },
    });

export type AppStore = ReturnType<typeof createStore>;

const store = createStore();

export default store;

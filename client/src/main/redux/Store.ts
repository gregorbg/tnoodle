import { configureStore } from "@reduxjs/toolkit";
import { informationSlice } from "./slice/InformationSlice";
import { scramblingSlice } from "./slice/ScramblingSlice";
import { settingsSlice } from "./slice/SettingsSlice";

const store = configureStore({
    reducer: {
        informationSlice: informationSlice.reducer,
        scramblingSlice: scramblingSlice.reducer,
        settingsSlice: settingsSlice.reducer,
    },
});

export default store;

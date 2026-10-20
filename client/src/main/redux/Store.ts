import { configureStore } from "@reduxjs/toolkit";
import { informationSlice } from "./slice/InformationSlice";
import { scramblingSlice } from "./slice/ScramblingSlice";

const store = configureStore({
    reducer: {
        informationSlice: informationSlice.reducer,
        scramblingSlice: scramblingSlice.reducer,
    },
});

export default store;

import { configureStore } from "@reduxjs/toolkit";
import { informationSlice } from "./slice/InformationSlice";

const store = configureStore({
    reducer: {
        informationSlice: informationSlice.reducer,
    },
});

export default store;

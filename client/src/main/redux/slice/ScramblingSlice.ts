import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

interface ScramblingState {
    competitionId?: string
    password: string;
    scramblingProgressCurrent: Record<string, number>;
    scramblingProgressTarget: Record<string, number>;
}

export const initialState: ScramblingState = {
    competitionId: undefined,
    password: "",
    scramblingProgressCurrent: {},
    scramblingProgressTarget: {},
};

export const scramblingSlice = createSlice({
    name: "scramblingSlice",
    initialState,
    reducers: {
        setCompetitionId: (state, action: PayloadAction<string | undefined>) => {
            state.competitionId = action.payload;
        },
        setPassword: (state, action: PayloadAction<string>) => {
            state.password = action.payload;
        },
        resetScramblingProgressCurrent: (state) => {
            state.scramblingProgressCurrent = {};
        },
        setScramblingProgressCurrentEvent: (
            state,
            action: PayloadAction<string>,
        ) => {
            state.scramblingProgressCurrent[action.payload] =
                (state.scramblingProgressCurrent[action.payload] || 0) + 1;
        },
        setScramblingProgressTarget: (
            state,
            action: PayloadAction<Record<string, number>>,
        ) => {
            state.scramblingProgressTarget = action.payload;
        },
    },
});

export const {
    setCompetitionId,
    setPassword,
    resetScramblingProgressCurrent,
    setScramblingProgressCurrentEvent,
    setScramblingProgressTarget,
} = scramblingSlice.actions;

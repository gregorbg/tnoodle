import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

interface ScramblerState {
    competitionId?: string;
    password: string;
    scramblingProgressCurrent: Record<string, number>;
    scramblingProgressTarget: Record<string, number>;
    showColorPicker: boolean;
    isUsingStaging: boolean;
}

export const initialState: ScramblerState = {
    competitionId: undefined,
    password: "",
    scramblingProgressCurrent: {},
    scramblingProgressTarget: {},
    showColorPicker: true,
    isUsingStaging: false,
};

export const scramblerSlice = createSlice({
    name: "scramblingSlice",
    initialState,
    reducers: {
        setCompetitionId: (
            state,
            action: PayloadAction<string | undefined>,
        ) => {
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
        setShowColorPicker: (state, action: PayloadAction<boolean>) => {
            state.showColorPicker = action.payload;
        },
        setIsUsingStaging: (state, action: PayloadAction<boolean>) => {
            state.isUsingStaging = action.payload;
        },
    },
});

export const {
    setCompetitionId,
    setPassword,
    resetScramblingProgressCurrent,
    setScramblingProgressCurrentEvent,
    setScramblingProgressTarget,
    setShowColorPicker,
    setIsUsingStaging,
} = scramblerSlice.actions;

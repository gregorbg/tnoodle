import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type WebsocketBlobResult from "../../model/WebsocketBlobResult";

interface ScramblingState {
    competitionId?: string
    fileZip?: WebsocketBlobResult;
    isManualSelection: boolean;
    generatingScrambles: boolean;
    password: string;
    scramblingProgressCurrent: Record<string, number>;
    scramblingProgressTarget: Record<string, number>;
}

const initialState: ScramblingState = {
    competitionId: undefined,
    fileZip: undefined,
    isManualSelection: true,
    generatingScrambles: false,
    password: "",
    scramblingProgressCurrent: {},
    scramblingProgressTarget: {},
};

export const scramblingSlice = createSlice({
    name: "scramblingSlice",
    initialState,
    reducers: {
        setCompetitionId: (state, action: PayloadAction<string>) => {
            state.competitionId = action.payload;
        },
        setFileZip: (
            state,
            action: PayloadAction<WebsocketBlobResult | undefined>,
        ) => {
            state.fileZip = action.payload;
        },
        setIsManualSelection: (state, action: PayloadAction<boolean>) => {
            state.isManualSelection = action.payload;
        },
        setGeneratingScrambles: (state, action: PayloadAction<boolean>) => {
            state.generatingScrambles = action.payload;
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
    setFileZip,
    setIsManualSelection,
    setPassword,
    setGeneratingScrambles,
    resetScramblingProgressCurrent,
    setScramblingProgressCurrentEvent,
    setScramblingProgressTarget,
} = scramblingSlice.actions;

import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface InformationState {
    selectedCompetitionId?: string;
    isManualSelection: boolean;
    showColorPicker: boolean;
}

const initialState: InformationState = {
    selectedCompetitionId: undefined,
    isManualSelection: true,
    showColorPicker: true,
};

export const informationSlice = createSlice({
    name: "informationSlice",
    initialState,
    reducers: {
        setSelectedCompetitionId: (state, action: PayloadAction<string | undefined>) => {
            state.selectedCompetitionId = action.payload;
        },
        setIsManualSelection: (state, action: PayloadAction<boolean>) => {
            state.isManualSelection = action.payload;
        },
        setShowColorPicker: (state, action: PayloadAction<boolean>) => {
            state.showColorPicker = action.payload;
        },
    },
});

export const {
    setIsManualSelection,
    setSelectedCompetitionId,
    setShowColorPicker,
} = informationSlice.actions;

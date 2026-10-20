import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface InformationState {
    selectedCompetitionId?: string;
    showColorPicker: boolean;
}

const initialState: InformationState = {
    selectedCompetitionId: undefined,
    showColorPicker: true,
};

export const informationSlice = createSlice({
    name: "informationSlice",
    initialState,
    reducers: {
        setSelectedCompetitionId: (
            state,
            action: PayloadAction<string | undefined>,
        ) => {
            state.selectedCompetitionId = action.payload;
        },
        setShowColorPicker: (state, action: PayloadAction<boolean>) => {
            state.showColorPicker = action.payload;
        },
    },
});

export const { setSelectedCompetitionId, setShowColorPicker } =
    informationSlice.actions;

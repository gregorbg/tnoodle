import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import Wcif from "../../model/Wcif";
import {
    copiesExtensionId,
    fmcTranslationsExtensionId,
    mbldCubesExtensionId,
    defaultWcif,
    getDefaultCopiesExtension,
} from "../../util/wcif.util";

interface WcifState {
    wcif: Wcif;
}

const initialState: WcifState = {
    wcif: { ...defaultWcif },
};

export const wcifSlice = createSlice({
    name: "wcifSlice",
    initialState,
    reducers: {
        setWcif: (state, action: PayloadAction<Wcif>) => {
            state.wcif = {
                ...action.payload,
                events: action.payload.events.map((event) => ({
                    ...event,
                    rounds: event.rounds.map((round) => ({
                        ...round,
                        extensions: [
                            ...round.extensions.filter(
                                (it) => it.id !== copiesExtensionId, // avoid duplicating extension
                            ),
                            getDefaultCopiesExtension(),
                        ],
                    })),
                    extensions: [
                        ...event.extensions.filter(
                            (it) =>
                                it.id !== fmcTranslationsExtensionId &&
                                it.id !== mbldCubesExtensionId,
                        ),
                    ],
                })),
            };
        },
    },
});

export const {
    setWcif,
} = wcifSlice.actions;

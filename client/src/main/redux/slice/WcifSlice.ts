import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import Wcif from "../../model/Wcif";
import WcifEvent from "../../model/WcifEvent";
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
        setWcifEvent: (state, action: PayloadAction<WcifEvent>) => {
            state.wcif = {
                ...state.wcif,
                events: [
                    ...state.wcif.events.filter(
                        (wcaEvent) => wcaEvent.id !== action.payload.id,
                    ),
                    action.payload,
                ],
            };
        },
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
    setWcifEvent,
    setWcif,
} = wcifSlice.actions;

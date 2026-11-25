import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { Wcif } from "../../api/wca.schemas";
import {
    copiesExtensionId,
    fmcTranslationsExtensionId,
    getDefaultCopiesExtension,
    mbldCubesExtensionId,
} from "../../util/extension.util.ts";
import { defaultWcif } from "../../util/wcif.util.ts";

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

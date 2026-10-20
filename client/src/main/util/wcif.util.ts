import Wcif from "../model/Wcif";
import WcifEvent from "../model/WcifEvent";
import { competitionName2Id, getDefaultCompetitionName } from "./competition.name.util";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import wcaApi from "../api/wca.api";
import { useCallback } from "react";
import type { Extension } from "../model/Extension";
import { setExtensionLazily } from "./extension.util";
import { useDispatch } from "react-redux";
import { setFileZip } from "../redux/slice/ScramblingSlice";

export const copiesExtensionId =
    "org.worldcubeassociation.tnoodle.SheetCopyCount";
export const colorSchemeExtensionId =
    "org.worldcubeassociation.tnoodle.ColorScheme";
export const frontendStatusExtensionId =
    "org.worldcubeassociation.tnoodle.CompetitionStatus";
export const fmcTranslationsExtensionId =
    "org.worldcubeassociation.tnoodle.FmcLanguages";
export const mbldCubesExtensionId =
    "org.worldcubeassociation.tnoodle.MultiScrambleCount";

/**
 * This is the default extension object the backend expects
 * @param {} copies
 */
export const getDefaultCopiesExtension = () => {
    return {
        id: copiesExtensionId,
        specUrl: "",
        data: {
            numCopies: 1,
        },
    };
};

export const buildMbldExtension = (mbld: number) => {
    return {
        id: mbldCubesExtensionId,
        specUrl: "",
        data: {
            requestedScrambles: mbld,
        },
    };
};

// Add 1 round of 3x3x3
const default333: WcifEvent = {
    id: "333",
    rounds: [
        {
            format: "a",
            id: "333-r1",
            scrambleSetCount: 1,
            extensions: [getDefaultCopiesExtension()],
        },
    ],
    extensions: [],
};

const name = getDefaultCompetitionName();
export const defaultWcif: Wcif = {
    formatVersion: "1.0",
    name,
    shortName: name,
    id: "",
    events: [default333],
    persons: [],
    schedule: { numberOfDays: 0, venues: [] },
    extensions: [],
};

const getWcifQueryKey = (competitionId?: string) => ["competitionWcif", competitionId];

export const useCompetitionWcif = (competitionId?: string) => {
    return useQuery({
        queryKey: getWcifQueryKey(competitionId),
        queryFn: async () => {
            const wcifResponse = await wcaApi.getCompetitionJson(competitionId!);
            const competitionWcif = wcifResponse.data;

            return {
                ...competitionWcif,
                events: competitionWcif.events.map((wcifEvent) => ({
                    ...wcifEvent,
                    rounds: wcifEvent.rounds.map((eventRound) => ({
                        ...eventRound,
                        extensions: [
                            ...eventRound.extensions.filter(
                                (it) => it.id !== copiesExtensionId,
                            ),
                            getDefaultCopiesExtension(),
                        ],
                    })),
                    extensions: wcifEvent.extensions.filter(
                        (it) =>
                            it.id !== fmcTranslationsExtensionId &&
                            it.id !== mbldCubesExtensionId,
                    ),
                })),
            };
        },
        enabled: competitionId !== undefined,
        initialData: defaultWcif,
    });
}

export const useCompetitionWcifDispatch = (competitionId?: string) => {
    const queryClient = useQueryClient();
    const dispatch = useDispatch();

    return useCallback((updater: (wcif: Wcif) => Wcif) => {
        // changing anything about the WCIF invalidates the currently generated ZIP file
        dispatch(setFileZip());

        return queryClient.setQueryData(
            getWcifQueryKey(competitionId),
            (wcif: Wcif) => ({
                ...wcif,
                ...updater(wcif),
            }),
        );
    }, [dispatch, queryClient, competitionId]);
}

export const useSetCompetitionName = (competitionId?: string) => {
    const dispatch = useCompetitionWcifDispatch(competitionId);

    return useCallback((name: string) => {
        return dispatch(wcif => ({ ...wcif, name, shortName: name, id: competitionName2Id(name) }));
    }, [dispatch]);
}

export const useSetWcifEvent = (competitionId?: string) => {
    const dispatch = useCompetitionWcifDispatch(competitionId);

    return useCallback((event: WcifEvent) => {
        return dispatch((wcif) => ({
            ...wcif,
            events: [
                ...wcif.events.filter(
                    (wcaEvent) => wcaEvent.id !== event.id,
                ),
                event,
            ],
        }));
    }, [dispatch]);
}

export const useSetEventExtension = (competitionId?: string) => {
    const setWcifEvent = useSetWcifEvent(competitionId);

    return useCallback(<T extends WcifEvent>(extendable: T, extensionId: string, buildExtension: () => Extension | null, middleware: (ext: T) => T = (ext) => ext) => {
        return setExtensionLazily(extendable, extensionId, buildExtension, (newExtendable) => setWcifEvent(middleware(newExtendable)));
    }, [setWcifEvent]);
}

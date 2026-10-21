import Wcif from "../model/Wcif";
import WcifEvent from "../model/WcifEvent";
import {
    competitionName2Id,
    getDefaultCompetitionName,
} from "./competition.name.util";
import {
    keepPreviousData,
    useQuery,
    useQueryClient,
} from "@tanstack/react-query";
import wcaApi from "../api/wca.api";
import { useCallback } from "react";
import type { Extension } from "../model/Extension";
import {
    buildFmcExtension,
    buildMbldExtension,
    copiesExtensionId,
    fmcTranslationsExtensionId,
    getDefaultCopiesExtension,
    mbldCubesExtensionId,
    setExtensionLazily,
} from "./extension.util";
import { useDispatch } from "react-redux";
import { setFileZip } from "../redux/slice/ScramblingSlice";
import tnoodleApi from "../api/tnoodle.api";

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

const getWcifQueryKey = (competitionId?: string) => [
    "competitionWcif",
    competitionId,
];

export const useCompetitionWcif = (competitionId?: string) => {
    return useQuery({
        queryKey: getWcifQueryKey(competitionId),
        queryFn: async ({ client }) => {
            const wcifResponse = await wcaApi.getCompetitionJson(
                competitionId!,
            );
            const wcif = wcifResponse.data;

            const [fmcTranslations, mbldScrambleCount] = await Promise.all([
                tnoodleApi.fetchSuggestedFmcTranslations(wcif.persons),
                tnoodleApi.fetchBestMbldAttempt(wcif.persons),
            ]);

            client.setQueryData(
                ["suggestedFmcTranslations", wcif.persons],
                fmcTranslations,
            );
            client.setQueryData(
                ["best-mbld-attempt", wcif.persons],
                mbldScrambleCount,
            );

            return {
                ...wcif,
                events: wcif.events.map((wcifEvent) => {
                    const roundsExtended = {
                        ...wcifEvent,
                        rounds: wcifEvent.rounds.map((eventRound) =>
                            setExtensionLazily(
                                eventRound,
                                copiesExtensionId,
                                getDefaultCopiesExtension,
                            ),
                        ),
                        extensions: wcifEvent.extensions.filter(
                            (it) =>
                                it.id !== fmcTranslationsExtensionId &&
                                it.id !== mbldCubesExtensionId,
                        ),
                    };

                    return setExtensionLazily(
                        setExtensionLazily(
                            roundsExtended,
                            fmcTranslationsExtensionId,
                            () => buildFmcExtension(fmcTranslations.data),
                        ),
                        mbldCubesExtensionId,
                        () =>
                            buildMbldExtension(
                                mbldScrambleCount.data.attempted,
                            ),
                    );
                }),
            };
        },
        enabled: competitionId !== undefined,
        initialData: () => competitionId === undefined ? defaultWcif : undefined,
        placeholderData: keepPreviousData,
    });
};

export const useCompetitionWcifDispatch = (competitionId?: string) => {
    const queryClient = useQueryClient();
    const dispatch = useDispatch();

    return useCallback(
        (updater: (wcif: Wcif) => Wcif) => {
            // changing anything about the WCIF invalidates the currently generated ZIP file
            dispatch(setFileZip());

            return queryClient.setQueryData(
                getWcifQueryKey(competitionId),
                (wcif: Wcif) => ({
                    ...wcif,
                    ...updater(wcif),
                }),
            );
        },
        [dispatch, queryClient, competitionId],
    );
};

export const useSetCompetitionName = (competitionId?: string) => {
    const dispatch = useCompetitionWcifDispatch(competitionId);

    return useCallback(
        (name: string) => {
            return dispatch((wcif) => ({
                ...wcif,
                name,
                shortName: name,
                id: competitionName2Id(name),
            }));
        },
        [dispatch],
    );
};

export const useSetWcifEvent = (competitionId?: string) => {
    const dispatch = useCompetitionWcifDispatch(competitionId);

    return useCallback(
        (event: WcifEvent) => {
            return dispatch((wcif) => ({
                ...wcif,
                events: [
                    ...wcif.events.filter(
                        (wcaEvent) => wcaEvent.id !== event.id,
                    ),
                    event,
                ],
            }));
        },
        [dispatch],
    );
};

export const useSetEventExtension = (competitionId?: string) => {
    const setWcifEvent = useSetWcifEvent(competitionId);

    return useCallback(
        <T extends WcifEvent>(
            extendable: T,
            extensionId: string,
            buildExtension: () => Extension | null,
            middleware: (ext: T) => T = (ext) => ext,
        ) => {
            const updatedExtension = setExtensionLazily(
                extendable,
                extensionId,
                buildExtension,
            );

            return setWcifEvent(middleware(updatedExtension));
        },
        [setWcifEvent],
    );
};

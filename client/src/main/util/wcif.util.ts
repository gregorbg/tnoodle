import {
    competitionName2Id,
    getDefaultCompetitionName,
} from "./competition.name.util";
import {
    keepPreviousData,
    useQuery,
    useQueryClient,
} from "@tanstack/react-query";
import { getWcifQueryKey } from "../api/wca.tanstack.ts";
import {
    buildFmcExtension,
    buildMbldExtension,
    copiesExtensionId,
    fmcTranslationsExtensionId,
    getDefaultCopiesExtension,
    mbldCubesExtensionId,
    setExtensionLazily,
} from "./extension.util.ts";
import { useCallback } from "react";
import type { Wcif, WcifEvent, WcifPerson } from "../api/wca.schemas.ts";
import { useTNoodleApi, useWcaApi } from "../api/client.ts";
import type { GetWcifVariables } from "../api/wca.ts";
import type { Extension } from "../model/Extension.ts";

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

const getWcifParams = (competitionId?: string, formatVersion?: string): GetWcifVariables | undefined => {
    if (competitionId === undefined || formatVersion === undefined) {
        return undefined;
    }

    return { path: { competitionId, formatVersion } };
};

export const getSuggestedFmcTranslationsKey = (persons: WcifPerson[]) => ["suggestedFmcTranslations", persons] as const;
export const getBestMbldAttemptKey = (persons: WcifPerson[]) => ["bestMbldAttempt", persons] as const;

export const useCompetitionWcif = (competitionId?: string) => {
    const wcaApiClient = useWcaApi();
    const wcifParams = getWcifParams(competitionId);

    const tnoodleApiClient = useTNoodleApi();

    return useQuery({
        queryKey: getWcifQueryKey(),
        queryFn: async ({ client, signal }) => {
            const wcif = await wcaApiClient.getWcif(wcifParams!, { signal });

            const [fmcTranslations, mbldScrambleCount] = await Promise.all([
                tnoodleApiClient.calculateSuggestedFmcTranslations({
                    body: wcif.persons,
                }, { signal }),
                tnoodleApiClient.calculateMbldBest({
                    body: wcif.persons,
                }, { signal }),
            ]);

            client.setQueryData(
                getSuggestedFmcTranslationsKey(wcif.persons),
                fmcTranslations,
            );
            client.setQueryData(
                getBestMbldAttemptKey(wcif.persons),
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
                            () => buildFmcExtension(fmcTranslations),
                        ),
                        mbldCubesExtensionId,
                        () =>
                            buildMbldExtension(
                                mbldScrambleCount.attempted,
                            ),
                    );
                }),
            };
        },
        enabled: competitionId !== undefined,
        initialData: () =>
            competitionId === undefined ? defaultWcif : undefined,
        placeholderData: keepPreviousData,
    });
};

export const useCompetitionWcifDispatch = (competitionId?: string) => {
    const queryClient = useQueryClient();

    return useCallback(
        (updater: (wcif: Wcif) => Wcif) => {
            return queryClient.setQueryData(
                getWcifQueryKey(getWcifParams(competitionId)),
                (wcif: Wcif) => ({
                    ...wcif,
                    ...updater(wcif),
                }),
            );
        },
        [queryClient, competitionId],
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

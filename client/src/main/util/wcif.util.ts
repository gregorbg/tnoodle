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
import { useCallback, useMemo } from "react";
import type { Wcif, WcifEvent, WcifPerson } from "../api/wca.schemas.ts";
import { useTNoodleApi, useTNoodleQueries, useWcaApi } from "../api/client.ts";
import type { GetWcifVariables } from "../api/wca.ts";
import type { Extension } from "../model/Extension.ts";
import { useSelector } from "react-redux";
import type RootState from "../model/RootState.ts";
import { MBLD_DEFAULT } from "../constants/wca.constants.ts";

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

export const defaultId = "%DEFAULT%";
export const defaultName = getDefaultCompetitionName();

export const defaultWcif: Wcif = {
    formatVersion: "1.0",
    name: defaultName,
    shortName: defaultName,
    id: defaultId,
    events: [default333],
    persons: [],
    schedule: { numberOfDays: 0, venues: [] },
    extensions: [],
};

const getWcifParams = (
    competitionId?: string,
    formatVersion?: string,
): GetWcifVariables | undefined => {
    if (
        competitionId === undefined ||
        formatVersion === undefined ||
        competitionId === defaultId
    ) {
        return undefined;
    }

    return { path: { competitionId, formatVersion } };
};

export const useIsManualGeneration = () => {
    const competitionId = useSelector(
        (state: RootState) => state.scramblingSlice.competitionId,
    );

    return competitionId === undefined || competitionId === defaultId;
};

export const getSuggestedFmcTranslationsKey = (persons: WcifPerson[]) =>
    ["suggestedFmcTranslations", persons] as const;
export const getBestMbldAttemptKey = (persons: WcifPerson[]) =>
    ["bestMbldAttempt", persons] as const;

export const useCompetitionWcif = () => {
    const competitionId = useSelector(
        (state: RootState) => state.scramblingSlice.competitionId,
    );

    const tnoodleQueries = useTNoodleQueries();
    const { data: backendFormatVersion } = useQuery(
        tnoodleQueries.getBackendWcifVersionOptions(),
    );

    return useCompetitionWcifQuery(competitionId, backendFormatVersion);
};

export const useCompetitionWcifQuery = (
    competitionId?: string,
    formatVersion?: string,
) => {
    const wcaApiClient = useWcaApi();
    const wcifParams = getWcifParams(competitionId, formatVersion);

    const tnoodleApiClient = useTNoodleApi();

    return useQuery({
        queryKey: getWcifQueryKey(wcifParams),
        queryFn: async ({ client, signal }) => {
            const wcif = await wcaApiClient.getWcif(wcifParams!, { signal });

            const [fmcTranslations, mbldScrambleCount] = await Promise.all([
                tnoodleApiClient.calculateSuggestedFmcTranslations(
                    {
                        body: wcif.persons,
                    },
                    { signal },
                ),
                tnoodleApiClient.calculateMbldBest(
                    {
                        body: wcif.persons,
                    },
                    { signal },
                ),
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
                    };

                    const fmcExtended = setExtensionLazily(
                        roundsExtended,
                        fmcTranslationsExtensionId,
                        () => buildFmcExtension(fmcTranslations),
                    );

                    return {
                        ...fmcExtended,
                        rounds: fmcExtended.rounds.map((eventRound) =>
                            setExtensionLazily(
                                eventRound,
                                mbldCubesExtensionId,
                                () =>
                                    buildMbldExtension(
                                        mbldScrambleCount?.attempted || MBLD_DEFAULT,
                                    ),
                            ),
                        ),
                    };
                }),
            };
        },
        enabled: competitionId !== undefined,
        initialData: () =>
            competitionId === undefined ? defaultWcif : undefined,
        placeholderData: keepPreviousData,
    });
};

const useCompetitionWcifDispatch = (
    competitionId?: string,
    formatVersion?: string,
) => {
    const queryClient = useQueryClient();

    const wcifParams = useMemo(() => {
        return getWcifParams(competitionId, formatVersion);
    }, [competitionId, formatVersion]);

    return useCallback(
        (updater: (wcif: Wcif) => Wcif) => {
            return queryClient.setQueryData(
                getWcifQueryKey(wcifParams),
                (wcif: Wcif) => ({
                    ...wcif,
                    ...updater(wcif),
                }),
            );
        },
        [queryClient, wcifParams],
    );
};

export const useSetCompetitionName = (wcif: Wcif) => {
    const dispatch = useCompetitionWcifDispatch(wcif.id, wcif.formatVersion);

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

export const useSetWcifEvent = (wcif: Wcif) => {
    const dispatch = useCompetitionWcifDispatch(wcif.id, wcif.formatVersion);

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

export const useSetEventExtension = (wcif: Wcif) => {
    const setWcifEvent = useSetWcifEvent(wcif);

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

import type { GetWcifVariables, Wcif, WcifEvent, WcifPerson } from "./wca.ts";
import { defaultId, defaultWcif } from "../util/wcif.util.ts";
import { useSelector } from "react-redux";
import type RootState from "../model/RootState.ts";
import { useTNoodleApi, useTNoodleQueries, useWcaApi } from "./client.ts";
import {
    keepPreviousData,
    useIsMutating,
    useQuery,
    useQueryClient,
} from "@tanstack/react-query";
import { getWcifQueryKey } from "./wca.tanstack.ts";
import {
    buildFmcExtension,
    buildMbldExtension,
    copiesExtensionId,
    fmcTranslationsExtensionId,
    getDefaultCopiesExtension,
    mbldCubesExtensionId,
    setExtensionLazily,
} from "../util/extension.util.ts";
import { MBLD_DEFAULT } from "../constants/wca.constants.ts";
import { useCallback, useMemo } from "react";
import { competitionName2Id } from "../util/competition.name.util.ts";
import type { Extension } from "../model/Extension.ts";

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
        (state: RootState) => state.scramblerSlice.competitionId,
    );

    return competitionId === undefined || competitionId === defaultId;
};

export const SCRAMBLING_MUTATION_KEY = ["scramble-zip"] as const;

export const useIsGeneratingScrambles = () => {
    const mutationCounter = useIsMutating({
        mutationKey: SCRAMBLING_MUTATION_KEY,
    });

    return mutationCounter > 0;
};

export const getSuggestedFmcTranslationsKey = (persons: WcifPerson[]) =>
    ["suggestedFmcTranslations", persons] as const;
export const getBestMbldAttemptKey = (persons: WcifPerson[]) =>
    ["bestMbldAttempt", persons] as const;

export const useCompetitionWcif = () => {
    const competitionId = useSelector(
        (state: RootState) => state.scramblerSlice.competitionId,
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
                                        mbldScrambleCount?.attempted ||
                                            MBLD_DEFAULT,
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

import Wcif from "../model/Wcif";
import WcifEvent from "../model/WcifEvent";
import { competitionName2Id, getDefaultCompetitionName } from "./competition.name.util";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import wcaApi from "../api/wca.api";
import { useCallback } from "react";

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
        queryFn: () => wcaApi.getCompetitionJson(competitionId!).then(resp => resp.data),
        enabled: competitionId !== undefined,
        initialData: defaultWcif,
    });
}

export const useCompetitionWcifDispatch = (competitionId?: string) => {
    const queryClient = useQueryClient();

    return useCallback((updater: (wcif: Wcif) => Wcif) => {
        return queryClient.setQueryData(
            getWcifQueryKey(competitionId),
            (wcif: Wcif) => ({
                ...wcif,
                ...updater(wcif),
            }),
        );
    }, [queryClient, competitionId]);
}

export const useSetCompetitionName = (competitionId?: string) => {
    const dispatch = useCompetitionWcifDispatch(competitionId);

    return useCallback((name: string) => {
        dispatch(wcif => ({ ...wcif, name, shortName: name, id: competitionName2Id(name) }));
    }, [dispatch]);
}

export const useSetWcifEvent = (competitionId?: string) => {
    const dispatch = useCompetitionWcifDispatch(competitionId);

    return useCallback((event: WcifEvent) => {
        dispatch((wcif) => ({
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

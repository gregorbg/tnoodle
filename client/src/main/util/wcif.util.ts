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

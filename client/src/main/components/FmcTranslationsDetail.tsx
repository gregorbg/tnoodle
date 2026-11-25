import { chunk } from "lodash";
import React, { useCallback, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import type RootState from "../model/RootState";
import type { WcifEvent } from "../api/wca.schemas";
import {
    defaultWcif,
    getSuggestedFmcTranslationsKey,
    useCompetitionWcif,
    useSetEventExtension,
} from "../util/wcif.util";
import {
    buildFmcExtension,
    findExtension,
    fmcTranslationsExtensionId,
} from "../util/extension.util";
import { useIsMutating, useQuery } from "@tanstack/react-query";
import { useTNoodleApi, useTNoodleQueries } from "../api/client.ts";

const TRANSLATIONS_PER_LINE = 3;

interface FmcTranslationsDetailProps {
    fmcWcifEvent: WcifEvent;
}

const FmcTranslationsDetail = ({
    fmcWcifEvent,
}: FmcTranslationsDetailProps) => {
    const competitionId = useSelector(
        (state: RootState) => state.scramblingSlice.competitionId,
    );

    const generatingScrambles =
        useIsMutating({ mutationKey: ["scramble-zip"] }) > 0;

    const { data: wcif = defaultWcif } = useCompetitionWcif();

    const tnoodleQueries = useTNoodleQueries();
    const { data: availableTranslations } = useQuery(tnoodleQueries.getFmcSheetLanguagesOptions());

    const tnoodleApiClient = useTNoodleApi();

    const { data: suggestedFmcTranslations } = useQuery({
        queryKey: getSuggestedFmcTranslationsKey(wcif.persons),
        queryFn: (options) => tnoodleApiClient.calculateSuggestedFmcTranslations({ body: wcif.persons }, options),
    });

    const [showTranslations, setShowTranslations] = useState(false);

    const selectedTranslations = useMemo<string[]>(() => {
        return (
            findExtension(fmcWcifEvent, fmcTranslationsExtensionId)?.data
                ?.languageTags ||
            suggestedFmcTranslations ||
            []
        );
    }, [fmcWcifEvent, suggestedFmcTranslations]);

    const setEventExtension = useSetEventExtension(competitionId);

    const updateEventSelectedTranslations = useCallback(
        (selectedTranslations: string[]) => {
            setEventExtension(
                fmcWcifEvent,
                fmcTranslationsExtensionId,
                () => buildFmcExtension(selectedTranslations),
            );
        },
        [setEventExtension, fmcWcifEvent],
    );

    const handleTranslation = (id: string, status: boolean) => {
        let newSelectedTranslations = selectedTranslations.filter(
            (it) => it !== id || status,
        );

        if (status && !newSelectedTranslations.includes(id)) {
            newSelectedTranslations.push(id);
        }

        updateEventSelectedTranslations(newSelectedTranslations);
    };

    const handleSelectAllTranslations = () => {
        if (availableTranslations === undefined) {
            selectNoneTranslation();
        } else {
            updateEventSelectedTranslations(
                availableTranslations.map((trans) => trans.code),
            );
        }
    };

    const selectNoneTranslation = () => {
        updateEventSelectedTranslations([]);
    };

    const selectSuggestedTranslations = () => {
        updateEventSelectedTranslations(suggestedFmcTranslations || []);
    };

    const translationsDetail = () => {
        if (availableTranslations === undefined) {
            return;
        }

        let translationsChunks = chunk(
            availableTranslations,
            TRANSLATIONS_PER_LINE,
        );

        return translationsChunks.map((translationsChunk, i) => (
            <tr key={i}>
                {translationsChunk.map((translation, j) => {
                    let checkboxId = `fmc-${translation.code}`;
                    let translationStatus = selectedTranslations.includes(
                        translation.code,
                    );

                    return (
                        <React.Fragment key={j}>
                            <th>
                                <label
                                    className="form-label text-capitalize"
                                    htmlFor={checkboxId}
                                >
                                    {translation.name}
                                </label>
                            </th>
                            <th>
                                <input
                                    disabled={generatingScrambles}
                                    type="checkbox"
                                    id={checkboxId}
                                    checked={translationStatus}
                                    onChange={(e) =>
                                        handleTranslation(
                                            translation.code,
                                            e.target.checked,
                                        )
                                    }
                                />
                            </th>
                            {j < TRANSLATIONS_PER_LINE - 1 && <th />}
                        </React.Fragment>
                    );
                })}
            </tr>
        ));
    };

    return (
        <tfoot>
            <tr>
                <th colSpan={4} className="text-center">
                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={() => setShowTranslations(!showTranslations)}
                    >
                        Translations
                    </button>
                </th>
            </tr>
            {showTranslations && (
                <>
                    <tr>
                        <th colSpan={4}>
                            <div className="btn-group">
                                <button
                                    type="button"
                                    className="btn btn-primary"
                                    onClick={handleSelectAllTranslations}
                                    disabled={generatingScrambles}
                                >
                                    Select All
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-primary"
                                    onClick={selectNoneTranslation}
                                    disabled={generatingScrambles}
                                >
                                    Select None
                                </button>
                                {!!suggestedFmcTranslations && (
                                    <button
                                        type="button"
                                        className="btn btn-primary"
                                        onClick={selectSuggestedTranslations}
                                        title="This selection is based on competitor's nationalities."
                                        disabled={generatingScrambles}
                                    >
                                        Select Suggested
                                    </button>
                                )}
                            </div>
                        </th>
                    </tr>
                    <tr>
                        <th colSpan={4} className="text-center">
                            <table className="table table-hover">
                                <tbody>{translationsDetail()}</tbody>
                            </table>
                        </th>
                    </tr>
                </>
            )}
        </tfoot>
    );
};

export default FmcTranslationsDetail;

import { chunk } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import RootState from "../model/RootState";
import tnoodleApi from "../api/tnoodle.api";
import WcifEvent from "../model/WcifEvent";
import {
    fmcTranslationsExtensionId,
    useCompetitionWcif,
    useSetEventExtension,
} from "../util/wcif.util";
import { findExtension } from "../util/extension.util";
import { useQuery } from "@tanstack/react-query";

const TRANSLATIONS_PER_LINE = 3;

interface FmcTranslationsDetailProps {
    fmcWcifEvent: WcifEvent;
}

const FmcTranslationsDetail = ({
    fmcWcifEvent,
}: FmcTranslationsDetailProps) => {
    const selectedCompetitionId = useSelector(
        (state: RootState) => state.informationSlice.selectedCompetitionId,
    );
    const generatingScrambles = useSelector(
        (state: RootState) => state.scramblingSlice.generatingScrambles,
    );

    const { data: wcif } = useCompetitionWcif(selectedCompetitionId);

    const { data: availableTranslations } = useQuery({
        queryKey: ["availableFmcTranslations"],
        queryFn: () => tnoodleApi.fetchAvailableFmcTranslations(),
        select: (resp) => resp.data,
    });

    const { data: suggestedFmcTranslations } = useQuery({
        queryKey: ["suggestedFmcTranslations", wcif.persons],
        queryFn: () => tnoodleApi.fetchSuggestedFmcTranslations(wcif.persons),
        select: (resp) => resp.data,
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

    const setEventExtension = useSetEventExtension(selectedCompetitionId);

    const buildFmcExtension = (selectedTranslations: string[]) => {
        return {
            id: fmcTranslationsExtensionId,
            specUrl: "",
            data: { languageTags: selectedTranslations },
        };
    };

    const updateEventSelectedTranslations = useCallback(
        (selectedTranslations: string[]) => {
            setEventExtension(
                fmcWcifEvent,
                fmcTranslationsExtensionId,
                () => buildFmcExtension(selectedTranslations),
            );
        },
        [fmcWcifEvent, setEventExtension],
    );

    useEffect(() => {
        const wcifExtension = findExtension(
            fmcWcifEvent,
            fmcTranslationsExtensionId,
        );

        if (
            wcifExtension === undefined &&
            suggestedFmcTranslations !== undefined
        ) {
            updateEventSelectedTranslations(suggestedFmcTranslations);
        }
    }, [
        fmcWcifEvent,
        updateEventSelectedTranslations,
        suggestedFmcTranslations,
    ]);

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
            updateEventSelectedTranslations(Object.keys(availableTranslations));
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

        let availableTranslationKeys = Object.keys(availableTranslations);

        let translationsChunks = chunk(
            availableTranslationKeys,
            TRANSLATIONS_PER_LINE,
        );

        return translationsChunks.map((translationsChunk, i) => (
            <tr key={i}>
                {translationsChunk.map((translation, j) => {
                    let checkboxId = `fmc-${translation}`;
                    let translationStatus =
                        selectedTranslations.includes(translation);

                    return (
                        <React.Fragment key={j}>
                            <th>
                                <label
                                    className="form-label text-capitalize"
                                    htmlFor={checkboxId}
                                >
                                    {availableTranslations[translation]}
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
                                            translation,
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

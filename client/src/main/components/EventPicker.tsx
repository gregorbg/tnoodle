import { OverlayTrigger, ProgressBar, Tooltip } from "react-bootstrap";
import { useSelector } from "react-redux";
import { MAX_WCA_ROUNDS, MBLD_DEFAULT } from "../constants/wca.constants";
import RootState from "../model/RootState";
import Round from "../model/Round";
import WcaEvent from "../model/WcaEvent";
import WcifEvent from "../model/WcifEvent";
import {
    defaultWcif,
    useCompetitionWcif,
    useSetEventExtension,
    useSetWcifEvent,
} from "../util/wcif.util";
import tnoodleApi from "../api/tnoodle.api";
import "./EventPicker.css";
import FmcTranslationsDetail from "./FmcTranslationsDetail";
import MbldDetail from "./MbldDetail";
import "@cubing/icons";
import { useCallback, useMemo, useState } from "react";
import SVG from "react-inlinesvg";
import SchemeColorPicker from "./SchemeColorPicker";
import _ from "lodash";
import {
    buildMbldExtension,
    colorSchemeExtensionId,
    copiesExtensionId,
    findExtension,
    getDefaultCopiesExtension,
} from "../util/extension.util";
import { useIsMutating, useQuery } from "@tanstack/react-query";

interface EventPickerProps {
    wcaEvent: WcaEvent;
    wcifEvent: WcifEvent;
    scramblingProgressTarget: Record<string, number>;
    scramblingProgressCurrent: Record<string, number>;
}

const EventPicker = ({ wcaEvent, wcifEvent, scramblingProgressTarget, scramblingProgressCurrent }: EventPickerProps) => {
    const selectedCompetitionId = useSelector(
        (state: RootState) => state.informationSlice.selectedCompetitionId,
    );
    const generatingScrambles =
        useIsMutating({ mutationKey: ["scramble-zip"] }) > 0;
    const showColorPicker = useSelector(
        (state: RootState) => state.informationSlice.showColorPicker,
    );

    const { data: wcaEvents } = useQuery({
        queryKey: ["wcaEvents"],
        queryFn: () => tnoodleApi.fetchWcaEvents(),
        select: (resp) => resp.data,
    });

    const { data: wcaFormats } = useQuery({
        queryKey: ["wcaFormats"],
        queryFn: () => tnoodleApi.fetchFormats(),
        select: (resp) => resp.data,
    });

    const { data: defaultColorScheme } = useQuery({
        queryKey: ["default-color-scheme", wcaEvent.puzzle_id],
        queryFn: () => tnoodleApi.fetchPuzzleColorScheme(wcaEvent.puzzle_id),
        select: (resp) => resp.data,
        enabled: wcifEvent.rounds.length > 0,
    });

    const { data: wcif = defaultWcif } = useCompetitionWcif(selectedCompetitionId);

    const [showColorSchemeConfig, setShowColorSchemeConfig] =
        useState<boolean>(false);

    const colorScheme = useMemo(() => {
        return (
            findExtension(wcifEvent, colorSchemeExtensionId)?.data
                ?.colorScheme || defaultColorScheme
        );
    }, [wcifEvent, defaultColorScheme]);

    const { data: puzzleSvg } = useQuery({
        queryKey: ["solved-puzzle-svg", wcaEvent.puzzle_id, colorScheme],
        queryFn: () =>
            tnoodleApi.fetchPuzzleSolvedSvg(wcaEvent.puzzle_id, colorScheme),
        select: (resp) => resp.data,
        enabled: colorScheme !== undefined,
    });

    const { data: randomSampleScramble, refetch: fetchRandomSampleScramble } =
        useQuery({
            queryKey: [
                "random-sample-scramble",
                wcaEvent.puzzle_id,
                colorScheme,
            ],
            queryFn: () =>
                tnoodleApi.fetchPuzzleRandomScramble(
                    wcaEvent.puzzle_id,
                    colorScheme,
                ),
            select: (resp) => resp.data,
            enabled: colorScheme !== undefined,
        });

    const fetchDisplayScramble = useCallback(
        (fetchNewScramble = true) => {
            // we need this additional boolean to make sure we can fetch only once
            // when the overlay is hidden, because its callback yields a `show` boolean.
            if (fetchNewScramble) {
                void fetchRandomSampleScramble();
            }
        },
        [fetchRandomSampleScramble],
    );

    const setWcifEvent = useSetWcifEvent(selectedCompetitionId);
    const setWcifEventExtension = useSetEventExtension(selectedCompetitionId);

    const updateEventRounds = (rounds: Round[]) => {
        let event = {
            ...wcifEvent,
            rounds,
        };

        setWcifEvent(event);
    };

    const updateEventColorScheme = (colorScheme: Record<string, string>) => {
        setWcifEventExtension(wcifEvent, colorSchemeExtensionId, () =>
            buildColorSchemeExtension(colorScheme),
        );
    };

    const buildColorSchemeExtension = (colorScheme: Record<string, string>) => {
        let isDefaultColorScheme = _.isEqual(colorScheme, defaultColorScheme);

        if (isDefaultColorScheme) {
            return null;
        }

        return {
            id: colorSchemeExtensionId,
            specUrl: "",
            data: { colorScheme },
        };
    };

    const generateDefaultRound = (wcaEvent: WcaEvent, numZeroBased: number) => {
        const extensions = wcaEvent.is_multiple_blindfolded
            ? [getDefaultCopiesExtension(), buildMbldExtension(MBLD_DEFAULT)]
            : [getDefaultCopiesExtension()];

        return {
            id: wcaEvent.id + "-r" + (numZeroBased + 1),
            format: wcaEvent.format_ids[0],
            scrambleSetCount: 1,
            extensions,
        };
    };

    const handleNumberOfRoundsChange = (
        numberOfRounds: number,
        rounds: Round[],
    ) => {
        let newRounds = [...rounds];
        // Ajust the number of rounds in case we have to remove
        while (newRounds.length > numberOfRounds) {
            newRounds.pop();
        }

        // case we have to add
        while (newRounds.length < numberOfRounds) {
            newRounds.push(generateDefaultRound(wcaEvent, newRounds.length));
        }
        updateEventRounds(newRounds);

        if (numberOfRounds === 0) {
            setShowColorSchemeConfig(false);
        }
    };

    const handleGeneralRoundChange = (
        roundNumber: number,
        value: string | number,
        rounds: Round[],
        name: "format" | "scrambleSetCount",
    ) => {
        updateEventRounds(
            rounds.map((round, i) =>
                i !== roundNumber ? round : { ...round, [name]: value },
            ),
        );
    };

    const handleNumberOfCopiesChange = (
        roundNumber: number,
        numCopies: number,
        rounds: Round[],
    ) => {
        updateEventRounds(
            rounds.map((round, i) =>
                i !== roundNumber
                    ? round
                    : {
                          ...round,
                          extensions: round.extensions.map((extension) =>
                              extension.id === copiesExtensionId
                                  ? { ...extension, data: { numCopies } }
                                  : extension,
                          ),
                      },
            ),
        );
    };

    const handleColorSchemeChange = (colorKey: string, hexColor: string) => {
        let newColorScheme = {
            ...colorScheme,
            [colorKey]: hexColor,
        };

        updateEventColorScheme(newColorScheme);
    };

    const abbreviate = (str: string) =>
        !!wcaFormats ? wcaFormats[str].shortName : "-";

    const updateForeignColorScheme = (wcaEvent: WcaEvent) => {
        let wcifEvent = wcif.events.find(
            (wcifEvent) => wcifEvent.id === wcaEvent.id,
        );

        if (wcifEvent !== undefined && colorScheme !== undefined) {
            setWcifEventExtension(wcifEvent, colorSchemeExtensionId, () =>
                buildColorSchemeExtension(colorScheme),
            );
        }
    };

    const maybeShowColorPicker = () => {
        if (!showColorSchemeConfig || wcifEvent.rounds.length === 0) {
            return;
        }

        if (colorScheme === undefined || defaultColorScheme === undefined) {
            return;
        }

        const defaultColors = _.uniq(Object.values(defaultColorScheme));

        const samePuzzleEvents =
            wcaEvents?.filter((event) => {
                return (
                    event.id !== wcaEvent.id &&
                    event.puzzle_id === wcaEvent.puzzle_id
                );
            }) ?? [];

        const samePuzzleGroupEvents =
            wcaEvents?.filter((event) => {
                return (
                    wcaEvent.puzzle_group_id !== null &&
                    event.id !== wcaEvent.id &&
                    event.puzzle_group_id === wcaEvent.puzzle_group_id
                );
            }) ?? [];

        return (
            <tr className="table-light">
                <th scope="col" colSpan={4}>
                    <table className={"table table-borderless"}>
                        <tbody>
                            <tr>
                                {Object.keys(colorScheme).map((colorKey) => {
                                    return (
                                        <td key={colorKey}>
                                            <SchemeColorPicker
                                                defaultColors={defaultColors}
                                                colorKey={colorKey}
                                                colorValue={
                                                    colorScheme[colorKey]
                                                }
                                                onColorChange={(hexColor) =>
                                                    handleColorSchemeChange(
                                                        colorKey,
                                                        hexColor,
                                                    )
                                                }
                                            />
                                        </td>
                                    );
                                })}
                            </tr>
                            <tr>
                                <td colSpan={defaultColors.length}>
                                    <button
                                        type="button"
                                        className="btn btn-warning me-4"
                                        onClick={() =>
                                            updateEventColorScheme(
                                                defaultColorScheme,
                                            )
                                        }
                                        disabled={generatingScrambles}
                                    >
                                        Reset to default
                                    </button>
                                    {samePuzzleEvents.length > 0 && (
                                        <button
                                            type="button"
                                            className="btn btn-secondary me-1"
                                            onClick={() =>
                                                samePuzzleEvents.forEach(
                                                    (wcaEvent) =>
                                                        updateForeignColorScheme(
                                                            wcaEvent,
                                                        ),
                                                )
                                            }
                                            disabled={generatingScrambles}
                                        >
                                            Propagate to puzzle
                                        </button>
                                    )}
                                    {samePuzzleGroupEvents.length > 0 && (
                                        <button
                                            type="button"
                                            className="btn btn-secondary me-1"
                                            onClick={() =>
                                                samePuzzleGroupEvents.forEach(
                                                    (wcaEvent) =>
                                                        updateForeignColorScheme(
                                                            wcaEvent,
                                                        ),
                                                )
                                            }
                                            disabled={generatingScrambles}
                                        >
                                            Propagate to group
                                        </button>
                                    )}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </th>
            </tr>
        );
    };

    const maybeShowTableTitles = () => {
        if (wcifEvent.rounds.length === 0) {
            return;
        }
        return (
            <tr className="table-light">
                <th scope="col">#</th>
                <th scope="col">Format</th>
                <th scope="col">Scramble Sets</th>
                <th scope="col">Copies</th>
            </tr>
        );
    };

    const maybeShowTableBody = () => {
        if (wcifEvent.rounds.length === 0) {
            return;
        }

        let wcifRounds = wcifEvent.rounds;

        return (
            <tbody>
                {Array.from({ length: wcifRounds.length }, (_, i) => {
                    let copies = wcifRounds[i].extensions.find(
                        (extension) => extension.id === copiesExtensionId,
                    )?.data.numCopies;

                    return (
                        <tr key={i}>
                            <th scope="row" className="align-middle">
                                {i + 1}
                            </th>
                            <td className="align-middle">
                                <select
                                    className="form-control"
                                    value={wcifRounds[i].format}
                                    onChange={(evt) =>
                                        handleGeneralRoundChange(
                                            i,
                                            evt.target.value,
                                            wcifRounds,
                                            "format",
                                        )
                                    }
                                    disabled={
                                        selectedCompetitionId !== undefined ||
                                        generatingScrambles
                                    }
                                >
                                    {wcaEvent.format_ids.map((format) => (
                                        <option key={format} value={format}>
                                            {abbreviate(format)}
                                        </option>
                                    ))}
                                </select>
                            </td>
                            <td>
                                <input
                                    className="form-control"
                                    type="number"
                                    value={wcifRounds[i].scrambleSetCount}
                                    onChange={(evt) =>
                                        handleGeneralRoundChange(
                                            i,
                                            Number(evt.target.value),
                                            wcifRounds,
                                            "scrambleSetCount",
                                        )
                                    }
                                    min={1}
                                    required
                                    disabled={
                                        selectedCompetitionId !== undefined ||
                                        generatingScrambles
                                    }
                                />
                            </td>
                            <td>
                                <input
                                    className="form-control"
                                    type="number"
                                    value={copies}
                                    onChange={(evt) =>
                                        handleNumberOfCopiesChange(
                                            i,
                                            Number(evt.target.value),
                                            wcifRounds,
                                        )
                                    }
                                    min={1}
                                    required
                                    disabled={generatingScrambles}
                                />
                            </td>
                        </tr>
                    );
                })}
            </tbody>
        );
    };

    const maybeShowProgressBar = () => {
        let target = scramblingProgressTarget[wcaEvent.id];

        if (wcifEvent.rounds.length === 0 || !generatingScrambles || !target) {
            return null;
        }

        let current = scramblingProgressCurrent[wcaEvent.id] || 0;

        let progress = (current / target) * 100;
        let miniThreshold = 2;

        if (progress === 0) {
            progress = miniThreshold;
        }

        return (
            <ProgressBar
                animated
                variant={progress === 100 ? "success" : "info"}
                now={progress}
                label={
                    progress === 100 || progress < miniThreshold
                        ? ""
                        : `${current} / ${target}`
                }
            />
        );
    };

    return (
        <table className="table table-sm shadow rounded overflow-hidden">
            <thead>
                <tr
                    className={
                        wcifEvent.rounds.length === 0
                            ? "table-dark text-white"
                            : "table-light"
                    }
                >
                    <th className="firstColumn" scope="col" />
                    <th scope="col" className="align-middle secondColumn">
                        <span
                            className={`cubing-icon event-${wcaEvent.id} unofficial-${wcaEvent.id}`}
                            title={wcaEvent.name}
                        />
                    </th>
                    <th className="align-middle lastTwoColumns" scope="col">
                        <h5 className="fw-bold">{wcaEvent.name}</h5>
                        {maybeShowProgressBar()}
                    </th>
                    <th className="lastTwoColumns" scope="col">
                        {wcifEvent.rounds.length > 0 &&
                            puzzleSvg !== undefined &&
                            randomSampleScramble !== undefined &&
                            showColorPicker && (
                                <div className="mb-2">
                                    <OverlayTrigger
                                        placement="left"
                                        onToggle={fetchDisplayScramble}
                                        overlay={
                                            <Tooltip className="fit-content">
                                                <SVG
                                                    src={
                                                        randomSampleScramble.svgImage
                                                    }
                                                    width={200}
                                                    height={200}
                                                />
                                                <p>
                                                    (click small preview to
                                                    edit)
                                                </p>
                                            </Tooltip>
                                        }
                                    >
                                        {({ ref, ...triggerHandler }) => (
                                            <SVG
                                                innerRef={ref}
                                                className={"lastTwoColumns"}
                                                src={puzzleSvg}
                                                height={50}
                                                onClick={() =>
                                                    setShowColorSchemeConfig(
                                                        !showColorSchemeConfig,
                                                    )
                                                }
                                                {...triggerHandler}
                                            />
                                        )}
                                    </OverlayTrigger>
                                </div>
                            )}
                        <label className="form-label">Rounds</label>
                        <select
                            className="form-select"
                            value={wcifEvent.rounds.length}
                            onChange={(evt) =>
                                handleNumberOfRoundsChange(
                                    Number(evt.target.value),
                                    wcifEvent.rounds,
                                )
                            }
                            disabled={
                                selectedCompetitionId !== undefined ||
                                generatingScrambles
                            }
                        >
                            {Array.from(
                                { length: MAX_WCA_ROUNDS + 1 },
                                (_, i) => (
                                    <option key={i} value={i}>
                                        {i}
                                    </option>
                                ),
                            )}
                        </select>
                    </th>
                </tr>
                {maybeShowColorPicker()}
                {maybeShowTableTitles()}
            </thead>
            {maybeShowTableBody()}
            {wcaEvent.is_multiple_blindfolded &&
                wcifEvent.rounds.length > 0 && (
                    <MbldDetail mbldWcifEvent={wcifEvent} />
                )}
            {wcaEvent.is_fewest_moves && wcifEvent.rounds.length > 0 && (
                <FmcTranslationsDetail fmcWcifEvent={wcifEvent} />
            )}
        </table>
    );
};

export default EventPicker;

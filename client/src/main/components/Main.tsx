import { FormEvent, useCallback, useMemo, useRef, useState } from "react";
import { useSelector } from "react-redux";
import tnoodleApi from "../api/tnoodle.api";
import wcaApi, { isUsingStaging } from "../api/wca.api";
import { ScrambleClient } from "../api/tnoodle.socket";
import RootState from "../model/RootState";
import EntryInterface from "./EntryInterface";
import EventPickerTable from "./EventPickerTable";
import Interceptor from "./Interceptor";
import "./Main.css";
import VersionInfo from "./VersionInfo";
import { defaultWcif, useCompetitionWcif } from "../util/wcif.util";
import {
    frontendStatusExtensionId,
    setExtensionLazily,
} from "../util/extension.util";
import { useMutation, useQuery } from "@tanstack/react-query";
import type Wcif from "../model/Wcif";
import _ from "lodash";

const Main = () => {
    const selectedCompetitionId = useSelector(
        (state: RootState) => state.informationSlice.selectedCompetitionId,
    );

    const interceptorRef = useRef<Interceptor>(null);

    const { data: wcaScrambleProgram } = useQuery({
        queryKey: ["wca-scramble-program"],
        queryFn: () => wcaApi.fetchVersionInfo(),
        select: (resp) => resp.data,
    });

    const { data: tnoodleVersion } = useQuery({
        queryKey: ["tnoodle-version"],
        queryFn: () => tnoodleApi.fetchRunningVersion(),
        select: (resp) => resp.data,
    });

    const runningVersion = `${tnoodleVersion?.projectName}-${tnoodleVersion?.projectVersion}`;
    const versionAllowed =
        wcaScrambleProgram?.allowed?.includes(runningVersion);
    const signatureValid =
        tnoodleVersion?.signedBuild &&
        tnoodleVersion?.signatureKeyBytes ===
            wcaScrambleProgram?.publicKeyBytes;

    const { data: wcif = defaultWcif } = useCompetitionWcif(
        selectedCompetitionId,
    );

    const [password, setPassword] = useState("");

    const [scramblingProgressTarget, setScramblingProgressTarget] = useState<
        Record<string, number>
    >({});
    const [scramblingProgressCurrent, setScramblingProgressCurrent] = useState<
        Record<string, number>
    >({});

    const setScramblingProgressCurrentEvent = useCallback(
        (eventId: string) =>
            setScramblingProgressCurrent((progress) => ({
                ...progress,
                [eventId]: (progress[eventId] || 0) + 1,
            })),
        [setScramblingProgressCurrent],
    );

    const extendedWcif = useMemo(
        () =>
            setExtensionLazily(wcif, frontendStatusExtensionId, () => ({
                id: frontendStatusExtensionId,
                specUrl: "",
                data: {
                    isStaging: isUsingStaging(),
                    isManual: selectedCompetitionId === undefined,
                    isSignedBuild: signatureValid,
                    isAllowedVersion: versionAllowed,
                },
            })),
        [wcif, selectedCompetitionId, signatureValid, versionAllowed],
    );

    const {
        data: fileZip,
        mutate: generateScrambles,
        isPending: generatingScrambles,
        variables,
    } = useMutation({
        mutationKey: ["scramble-zip"],
        mutationFn: async ({
            submitWcif,
            submitPassword,
        }: {
            submitWcif: Wcif;
            submitPassword: string;
        }) => {
            const scrambleClient = new ScrambleClient(
                setScramblingProgressTarget,
                setScramblingProgressCurrentEvent,
            );

            return await tnoodleApi.fetchZip(
                scrambleClient,
                submitWcif,
                submitPassword,
            );
        },
        onError: interceptorRef.current?.updateMessage,
        onSettled: () => setScramblingProgressCurrent({}),
    });

    const onSubmit = (evt: FormEvent<HTMLFormElement>) => {
        evt.preventDefault();

        if (generatingScrambles) {
            return;
        }

        const inputsChanged =
            variables?.submitPassword !== password ||
            !_.isEqual(variables?.submitWcif, extendedWcif);

        if (!inputsChanged && fileZip !== undefined) {
            tnoodleApi.convertToBlob(fileZip).then((blob) => downloadZip(blob));
        } else {
            generateScrambles({
                submitWcif: extendedWcif,
                submitPassword: password,
            });
        }
    };

    const downloadZip = (blob: Blob) => {
        // We use the unofficialZip to stamp .zip in order to prevent delegates / organizers mistakes.
        // If TNoodle version is not official (as per VersionInfo) or if we generate scrambles using
        // a competition from staging, add a [Unofficial]
        let officialZipStatus = signatureValid && versionAllowed;

        let isUnofficialZip =
            !officialZipStatus ||
            (selectedCompetitionId !== undefined && isUsingStaging());

        let fileName =
            (isUnofficialZip ? "[UNOFFICIAL] " : "") + wcif.name + ".zip";

        const link = document.createElement("a");
        link.href = URL.createObjectURL(blob);
        link.download = fileName;
        link.target = "_blank";
        link.setAttribute("type", "hidden");

        // This is needed for firefox
        document.body.appendChild(link);

        link.click();
        link.remove();
    };

    const appOnlineMode = !!process.env.REACT_APP_TNOODLE_ONLINE_MODE;

    const scrambleButton = () => {
        if (generatingScrambles) {
            return (
                <button
                    className="btn btn-primary button-transparent form-control"
                    title="Wait until the process is done"
                    disabled
                >
                    Generating Scrambles
                </button>
            );
        }
        if (!!fileZip) {
            return (
                <button type="submit" className="btn btn-success form-control">
                    Download Scrambles
                </button>
            );
        }

        // At least 1 events must have at least 1 round.
        let anyEventHasRounds = wcif.events.some(
            (event) => event.rounds.length > 0,
        );

        let isLoggedCompSelection =
            selectedCompetitionId !== undefined && wcaApi.isLogged();
        let isOnlineLocked = appOnlineMode && !isLoggedCompSelection;

        let disableScrambleButton = !anyEventHasRounds || isOnlineLocked;

        // In case the user did not select any events, we make the button a little more transparent than disabled
        let btnClass =
            "btn btn-primary form-control" +
            (disableScrambleButton ? " button-transparent" : "");

        return (
            <button
                type="submit"
                className={btnClass}
                disabled={disableScrambleButton}
                title={
                    !anyEventHasRounds
                        ? "No events selected."
                        : isOnlineLocked
                          ? "You must be logged in and pick a competition"
                          : ""
                }
            >
                Generate Scrambles
            </button>
        );
    };

    return (
        <form onSubmit={onSubmit}>
            <div className="sticky-top bg-light">
                <Interceptor ref={interceptorRef} />
                <VersionInfo />
                <div className="container-fluid pt-2">
                    <div className="row g-3 pb-3 align-items-end">
                        <EntryInterface
                            password={password}
                            setPassword={setPassword}
                        />
                        <div className="col-sm-4">{scrambleButton()}</div>
                    </div>
                </div>
            </div>
            <EventPickerTable
                scramblingProgressTarget={scramblingProgressTarget}
                scramblingProgressCurrent={scramblingProgressCurrent}
            />
        </form>
    );
};

export default Main;

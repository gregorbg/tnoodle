import { type FormEvent, useMemo, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { ScrambleClient } from "../api/tnoodle.socket";
import { isUsingStaging } from "../api/wca.oidc";
import type RootState from "../model/RootState";
import {
    resetScramblingProgressCurrent,
    setScramblingProgressCurrentEvent,
    setScramblingProgressTarget,
} from "../redux/slice/ScramblingSlice";
import EntryInterface from "./EntryInterface";
import EventPickerTable from "./EventPickerTable";
import Interceptor from "./Interceptor";
import "./Main.css";
import VersionInfo from "./VersionInfo";
import { useAuth } from "react-oidc-context";
import { convertToBlob } from "../util/data.util.ts";
import { frontendStatusExtensionId } from "../util/extension.util.ts";
import { defaultWcif, useCompetitionWcif } from "../util/wcif.util.ts";
import type { Wcif } from "../api/wca.schemas.ts";
import { useMutation } from "@tanstack/react-query";
import _ from "lodash";

const Main = () => {
    const competitionId = useSelector(
        (state: RootState) => state.scramblingSlice.competitionId,
    );
    const password = useSelector(
        (state: RootState) => state.scramblingSlice.password,
    );

    const { data: wcif = defaultWcif } = useCompetitionWcif(competitionId);

    const interceptorRef = useRef<Interceptor>(null);

    const oidcAuth = useAuth();

    const dispatch = useDispatch();

    const onScrambleHandShake = (payload: Record<string, number>) =>
        dispatch(setScramblingProgressTarget(payload));

    const onScrambleProgress = (eventId: string) =>
        dispatch(setScramblingProgressCurrentEvent(eventId));

    const onScrambleCompleted = () =>
        dispatch(resetScramblingProgressCurrent());

    const {
        data: fileZip,
        mutate: generateZip,
        isPending: generatingScrambles,
        variables,
    } = useMutation({
        mutationKey: ["scramble-zip"] as const,
        mutationFn: async ({
            submitWcif,
            submitPassword,
        }: {
            submitWcif: Wcif;
            submitPassword: string;
        }) => {
            const scrambleClient = new ScrambleClient(
                onScrambleHandShake,
                onScrambleProgress,
            );

            return scrambleClient.loadScrambles({
                wcif: submitWcif,
                zipPassword: !submitPassword ? null : submitPassword,
            }, submitWcif.id);
        },
        onError: interceptorRef.current?.updateMessage,
        onSettled: onScrambleCompleted,
    });

    const inputsChanged = useMemo(
        () =>
            variables?.submitPassword !== password ||
            !_.isEqual(variables?.submitWcif, wcif),
        [wcif, password, variables],
    );

    const onSubmit = (evt: FormEvent) => {
        evt.preventDefault();

        if (generatingScrambles) {
            return;
        }

        if (fileZip !== undefined && !inputsChanged) {
            convertToBlob(fileZip).then((blob) => downloadZip(blob));
        } else {
            generateZip({
                submitWcif: wcif,
                submitPassword: password,
            });
        }
    };

    const downloadZip = (blob: Blob) => {
        // We use the unofficialZip to stamp .zip in order to prevent delegates / organizers mistakes.
        // If TNoodle version is not official (as per VersionInfo) or if we generate scrambles using
        // a competition from staging, add a [Unofficial]
        let frontendStatusExtension = wcif.extensions.find(
            (ext) => ext.id === frontendStatusExtensionId,
        );

        let isValidSignedBuild = frontendStatusExtension?.data.isSignedBuild;
        let isAllowedVersion = frontendStatusExtension?.data.isAllowedVersion;

        let officialZipStatus = isValidSignedBuild && isAllowedVersion;

        let isUnofficialZip =
            !officialZipStatus || (!!competitionId && isUsingStaging());

        let fileName =
            (isUnofficialZip ? "[UNOFFICIAL] " : "") +
            variables?.submitWcif.id +
            ".zip";

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

    const appOnlineMode = !!import.meta.env.VITE_TNOODLE_ONLINE_MODE;

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
            !!competitionId && oidcAuth.isAuthenticated;
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
                        <EntryInterface />
                        <div className="col-sm-4">{scrambleButton()}</div>
                    </div>
                </div>
            </div>
            <EventPickerTable />
        </form>
    );
};

export default Main;

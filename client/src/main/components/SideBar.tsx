import { useCallback, useEffect, useMemo, useState } from "react";
import { Collapse, Spinner } from "react-bootstrap";
import { useDispatch, useSelector } from "react-redux";
import logo from "../assets/tnoodle_logo.svg";
import type RootState from "../model/RootState";
import {
    setCompetitionId,
} from "../redux/slice/ScramblingSlice";
import { getDefaultCompetitionName } from "../util/competition.name.util";
import {
    deleteParameter,
    getQueryParameter,
    setQueryParameter,
} from "../util/query.param.util";
import { defaultWcif, useCompetitionWcif } from "../util/wcif.util";
import "./SideBar.css";
import { setShowColorPicker } from "../redux/slice/SettingsSlice";
import { useAuth } from "react-oidc-context";
import type { CustomAuthState } from "../api/wca.oidc";
import { useWcaQueries } from "../api/client.ts";
import { useIsMutating, useQuery } from "@tanstack/react-query";

const SideBar = () => {
    const showColorPicker = useSelector(
        (state: RootState) => state.settingsSlice.showColorPicker,
    );

    const generatingScrambles =
        useIsMutating({ mutationKey: ["scramble-zip"] }) > 0;

    const { data: wcif = defaultWcif, isLoading: loadingCompetitionInfo } = useCompetitionWcif()

    const [isOpen, setIsOpen] = useState(true);

    const handleIsOpen = useCallback(
        () => setIsOpen(window.innerWidth > 992),
        [setIsOpen],
    );

    useEffect(() => {
        window.addEventListener("resize", handleIsOpen);

        return () => window.removeEventListener("resize", handleIsOpen);
    }, [handleIsOpen]);

    const oidcAuth = useAuth();
    const wcaQueries = useWcaQueries();

    const { data: user, isLoading: loadingUser } = useQuery({
        ...wcaQueries.getMeOptions(),
        enabled: oidcAuth.isAuthenticated,
    });

    const { data: upcomingCompetitions, isLoading: loadingCompetitions } =
        useQuery({
            ...wcaQueries.getCompetitionsOptions({
                query: {
                    managed_by_me: true,
                    start: new Date(
                        Date.now() - 7 * 24 * 60 * 60 * 1000,
                    ).toISOString(),
                    sort: "start_date",
                },
            }),
            enabled: oidcAuth.isAuthenticated,
        });

    const competitions = useMemo(() => {
        if (
            wcif.id === defaultWcif.id ||
            wcif.name === getDefaultCompetitionName()
        ) {
            return upcomingCompetitions || [];
        }

        if (upcomingCompetitions === undefined) {
            return [{ id: wcif.id, name: wcif.name }];
        }

        const queryParamId = getQueryParameter("competitionId");
        const isUpcoming = upcomingCompetitions.some(
            (comp) => comp.id === queryParamId,
        );

        if (isUpcoming) {
            return upcomingCompetitions;
        } else {
            return [...upcomingCompetitions, { id: wcif.id, name: wcif.name }];
        }
    }, [wcif, upcomingCompetitions]);

    const dispatch = useDispatch();

    const pluralize = (string: string, number: number) =>
        string + (number > 1 ? "s" : "");

    const handleManualSelection = useCallback(() => {
        dispatch(setCompetitionId(undefined));
        deleteParameter("competitionId");
    }, [dispatch]);

    const handleCompetitionSelection = useCallback(
        (competitionId: string) => {
            setQueryParameter("competitionId", competitionId);
            dispatch(setCompetitionId(competitionId));
        },
        [dispatch],
    );

    const logInButton = () => {
        return (
            <div className="vstack gap-2">
                {oidcAuth.isAuthenticated ? (
                    <button
                        type="button"
                        className="btn btn-primary btn-lg"
                        onClick={() => oidcAuth.removeUser()}
                        disabled={generatingScrambles}
                    >
                        Log Out
                    </button>
                ) : (
                    <button
                        type="button"
                        className="btn btn-primary btn-lg"
                        onClick={() =>
                            oidcAuth.signinRedirect({
                                state: {
                                    returnSearch: window.location.search,
                                } satisfies CustomAuthState,
                            })
                        }
                        disabled={generatingScrambles}
                    >
                        Log In
                    </button>
                )}
                {!!user?.me && (
                    <p className="text-white">
                        Welcome, {user.me.name}.
                        {!!competitions &&
                            ` You have ${
                                competitions.length
                            } manageable ${pluralize(
                                " competition",
                                competitions.length,
                            )} upcoming.`}
                    </p>
                )}
            </div>
        );
    };

    const loadingElement = (text: string) => (
        <div className="text-white">
            <Spinner animation="border" role="status" />
            <p>{text}...</p>
        </div>
    );

    const loadingArea = () => {
        if (loadingUser) {
            return loadingElement("Loading user");
        }

        if (loadingCompetitions) {
            return loadingElement("Loading competitions");
        }

        if (loadingCompetitionInfo) {
            return loadingElement("Loading competition information");
        }
    };

    const settingsArea = () => (
        <div className="form-check form-switch text-white">
            <input
                type="checkbox"
                role="switch"
                className="form-check-input"
                id="colorPicker"
                disabled={generatingScrambles}
                checked={showColorPicker}
                onChange={(e) => dispatch(setShowColorPicker(e.target.checked))}
            />
            <label className="form-check-label" htmlFor="colorPicker">
                Show color scheme pickers
            </label>
        </div>
    );

    return (
        <div className="h-100 pb-2">
            <div className="d-flex flex-lg-column align-items-center overflow-hidden">
                <img
                    className="tnoodle-logo mt-2"
                    src={logo}
                    alt="TNoodle logo"
                />
                <h1 className="display-3" id="title">
                    TNoodle
                </h1>
                <button
                    type="button"
                    className="btn btn-primary btn-lg btn-outline-light ms-auto d-lg-none"
                    onClick={() => setIsOpen(!isOpen)}
                    disabled={generatingScrambles}
                    aria-label="Toggle menu"
                    aria-expanded={isOpen}
                >
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 30 30"
                        width={30}
                        height={30}
                    >
                        <path
                            stroke="currentColor"
                            strokeWidth={2}
                            strokeLinecap="round"
                            strokeMiterlimit={10}
                            d="M4 7h22M4 15h22M4 23h22"
                        />
                    </svg>
                </button>
            </div>
            <Collapse in={isOpen} className="mt-2">
                <div className="vstack gap-2">
                    {settingsArea()}
                    {!!competitions && (
                        <div className="vstack gap-2">
                            {competitions.length > 0 && (
                                <button
                                    id="manual-selection-button"
                                    type="button"
                                    className="btn btn-primary btn-lg btn-outline-light"
                                    onClick={handleManualSelection}
                                    disabled={generatingScrambles}
                                >
                                    Manual Selection
                                </button>
                            )}
                            <div className="btn-group-vertical" role="group">
                                {competitions.map((competition) => (
                                    <button
                                        key={competition.id}
                                        type="button"
                                        className="btn btn-primary btn-lg"
                                        disabled={generatingScrambles}
                                        onClick={() =>
                                            handleCompetitionSelection(
                                                competition.id,
                                            )
                                        }
                                    >
                                        {competition.name}
                                    </button>
                                ))}
                            </div>
                            {loadingArea()}
                        </div>
                    )}
                    {logInButton()}
                </div>
            </Collapse>
        </div>
    );
};

export default SideBar;

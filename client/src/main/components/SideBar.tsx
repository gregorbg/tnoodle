import { useCallback, useEffect, useMemo, useState } from "react";
import { Collapse, Spinner } from "react-bootstrap";
import { useDispatch, useSelector } from "react-redux";
import wcaApi from "../api/wca.api";
import logo from "../assets/tnoodle_logo.svg";
import RootState from "../model/RootState";
import {
    setSelectedCompetitionId,
    setShowColorPicker,
} from "../redux/slice/InformationSlice";
import { setFileZip } from "../redux/slice/ScramblingSlice";
import {
    deleteParameter,
    getQueryParameter,
    setQueryParameter,
} from "../util/query.param.util";
import { defaultWcif, useCompetitionWcif } from "../util/wcif.util";
import "./SideBar.css";
import { useQuery } from "@tanstack/react-query";

const SideBar = () => {
    const selectedCompetitionId = useSelector(
        (state: RootState) => state.informationSlice.selectedCompetitionId,
    );
    const generatingScrambles = useSelector(
        (state: RootState) => state.scramblingSlice.generatingScrambles,
    );
    const showColorPicker = useSelector(
        (state: RootState) => state.informationSlice.showColorPicker,
    );

    const { data: me, isFetching: loadingUser } = useQuery({
        queryKey: ["me"],
        queryFn: wcaApi.fetchMe,
        select: (axios) => axios.data.me,
        enabled: wcaApi.isLogged(),
    });

    const { data: upcomingCompetitions, isFetching: loadingCompetitions } =
        useQuery({
            queryKey: ["upcoming-competitions"],
            queryFn: wcaApi.getUpcomingManageableCompetitions,
            select: (axios) => axios.data,
            enabled: wcaApi.isLogged(),
        });

    const { data: wcif = defaultWcif, isFetching: loadingCompetitionInfo, isPlaceholderData } =
        useCompetitionWcif(selectedCompetitionId);

    const [isOpen, setIsOpen] = useState(true);

    const handleIsOpen = useCallback(
        () => setIsOpen(window.innerWidth > 992),
        [setIsOpen],
    );

    useEffect(() => {
        window.addEventListener("resize", handleIsOpen);

        return () => window.removeEventListener("resize", handleIsOpen);
    }, [handleIsOpen]);

    const competitions = useMemo(() => {
        if (selectedCompetitionId === undefined) {
            return upcomingCompetitions || [];
        }

        if (upcomingCompetitions === undefined) {
            return [wcif];
        }

        const isUpcoming = upcomingCompetitions.some(
            (comp) => comp.id === selectedCompetitionId,
        );

        if (isUpcoming) {
            return upcomingCompetitions;
        } else {
            return [...upcomingCompetitions, wcif];
        }
    }, [selectedCompetitionId, upcomingCompetitions, wcif]);

    const dispatch = useDispatch();

    const pluralize = (string: string, number: number) =>
        string + (number > 1 ? "s" : "");

    const handleManualSelection = () => {
        dispatch(setSelectedCompetitionId(undefined));
        dispatch(setFileZip());

        deleteParameter("competitionId");
    };

    useEffect(() => {
        const queryParamId = getQueryParameter("competitionId");

        if (queryParamId !== null && wcif.id !== queryParamId) {
            dispatch(setSelectedCompetitionId(queryParamId));
        }
    }, [dispatch, wcif.id]);

    const handleCompetitionSelection = useCallback(
        (competitionId: string) => {
            setQueryParameter("competitionId", competitionId);
            dispatch(setSelectedCompetitionId(competitionId));
        },
        [dispatch],
    );

    const logInButton = () => {
        return (
            <div className="vstack gap-2">
                <button
                    type="button"
                    className="btn btn-primary btn-lg"
                    onClick={wcaApi.isLogged() ? wcaApi.logOut : wcaApi.logIn}
                    disabled={generatingScrambles}
                >
                    {wcaApi.isLogged() ? "Log Out" : "Log In"}
                </button>
                {!!me && (
                    <p className="text-white">
                        Welcome, {me.name}.
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

        if (loadingCompetitionInfo && isPlaceholderData) {
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

import { chunk } from "lodash";
import { useCallback, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import tnoodleApi from "../api/tnoodle.api";
import { toWcaUrl } from "../api/wca.api";
import RootState from "../model/RootState";
import { setTranslations } from "../redux/slice/FmcSlice";
import { setWcaEvents, setWcaFormats } from "../redux/slice/WcifSlice";
import EventPicker from "./EventPicker";

const EVENTS_PER_LINE = 2;

const EventPickerTable = () => {
    const competitionId = useSelector(
        (state: RootState) => state.competitionSlice.competitionId
    );
    const wcif = useSelector((state: RootState) => state.wcifSlice.wcif);
    const wcaEvents = useSelector(
        (state: RootState) => state.wcifSlice.wcaEvents
    );
    const editingStatus = useSelector(
        (state: RootState) => state.wcifSlice.editingStatus
    );

    const dispatch = useDispatch();

    const getFmcTranslations = useCallback(() => {
        tnoodleApi.fetchAvailableFmcTranslations().then((response) => {
            const translations = Object.entries(response.data).map(
                ([id, name]) => ({
                    id,
                    name,
                    status: true,
                })
            );
            dispatch(setTranslations(translations));
        });
    }, [dispatch]);

    const fetchInformation = () => {
        tnoodleApi.fetchFormats().then((response) => {
            dispatch(setWcaFormats(response.data));
        });
        tnoodleApi
            .fetchWcaEvents()
            .then((response) => dispatch(setWcaEvents(response.data)));
        getFmcTranslations();
    };

    useEffect(fetchInformation, [dispatch, getFmcTranslations]);

    const maybeShowEditWarning = () => {
        if (!competitionId) {
            return;
        }
        return (
            <div className="row">
                <div className="col-12">
                    <p>
                        Found {wcif.events.length} event
                        {wcif.events.length > 1 ? "s" : ""} for {wcif.name}.
                    </p>
                    <p>
                        You can view and change the rounds over on{" "}
                        <a
                            href={toWcaUrl(
                                `/competitions/${competitionId}/events/edit`
                            )}
                        >
                            {" "}
                            the WCA
                        </a>
                        .{" "}
                        <strong>
                            Refresh this page after making any changes on the
                            WCA website.
                        </strong>
                    </p>
                </div>
            </div>
        );
    };

    // Prevent from remembering previous order
    if (!wcaEvents) {
        return null;
    }

    // This filters events to show only those in the competition.
    let filteredEvents = wcaEvents.filter(
        (wcaEvent) =>
            editingStatus || wcif.events.find((item) => item.id === wcaEvent.id)
    );

    let eventChunks = chunk(filteredEvents, EVENTS_PER_LINE);

    return (
        <div className="container-fluid mt-2">
            {maybeShowEditWarning()}
            {eventChunks.map((chunk, i) => {
                return (
                    <div className="row p-0" key={i}>
                        {chunk.map((wcaEvent) => {
                            return (
                                <div className="col-lg-6" key={wcaEvent.id}>
                                    <EventPicker
                                        wcaEvent={wcaEvent}
                                        wcifEvent={wcif.events.find(
                                            (item) => item.id === wcaEvent.id
                                        )}
                                    />
                                </div>
                            );
                        })}
                    </div>
                );
            })}
        </div>
    );
};

export default EventPickerTable;

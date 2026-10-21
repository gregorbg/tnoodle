import { chunk } from "lodash";
import tnoodleApi from "../api/tnoodle.api";
import { toWcaUrl } from "../api/wca.api";
import EventPicker from "./EventPicker";
import WcaEvent from "../model/WcaEvent";
import { defaultWcif, useCompetitionWcif } from "../util/wcif.util";
import { MBLD_DEFAULT } from "../constants/wca.constants";
import { useQuery } from "@tanstack/react-query";
import { buildMbldExtension } from "../util/extension.util";

const EVENTS_PER_LINE = 2;

const EventPickerTable = ({
    competitionId,
    showColorPicker,
    scramblingProgressTarget,
    scramblingProgressCurrent,
}: {
    competitionId?: string;
    showColorPicker: boolean;
    scramblingProgressTarget: Record<string, number>;
    scramblingProgressCurrent: Record<string, number>;
}) => {
    const { data: wcif = defaultWcif } = useCompetitionWcif(
        competitionId,
    );

    const { data: wcaEvents } = useQuery({
        queryKey: ["wcaEvents"],
        queryFn: () => tnoodleApi.fetchWcaEvents(),
        select: (resp) => resp.data,
    });

    const generateDefaultEvent = (wcaEvent: WcaEvent) => {
        const extensions = wcaEvent.is_multiple_blindfolded
            ? [buildMbldExtension(MBLD_DEFAULT)]
            : [];

        return {
            id: wcaEvent.id,
            rounds: [],
            extensions,
        };
    };

    const maybeShowEditWarning = () => {
        if (competitionId === undefined) {
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
                                `/competitions/${wcif.id}/events/edit`,
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
            competitionId === undefined ||
            wcif.events.find((item) => item.id === wcaEvent.id),
    );

    let eventChunks = chunk(filteredEvents, EVENTS_PER_LINE);

    return (
        <div className="container-fluid mt-2">
            {maybeShowEditWarning()}
            {eventChunks.map((chunk, i) => {
                return (
                    <div className="row p-0" key={i}>
                        {chunk.map((wcaEvent) => {
                            let wcifEvent =
                                wcif.events.find(
                                    (item) => item.id === wcaEvent.id,
                                ) || generateDefaultEvent(wcaEvent);

                            return (
                                <div className="col-lg-6" key={wcaEvent.id}>
                                    <EventPicker
                                        competitionId={competitionId}
                                        wcaEvent={wcaEvent}
                                        wcifEvent={wcifEvent}
                                        showColorPicker={showColorPicker}
                                        scramblingProgressTarget={
                                            scramblingProgressTarget
                                        }
                                        scramblingProgressCurrent={
                                            scramblingProgressCurrent
                                        }
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

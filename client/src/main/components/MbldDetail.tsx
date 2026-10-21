import { useSelector } from "react-redux";
import { MBLD_DEFAULT, MBLD_MIN } from "../constants/wca.constants";
import RootState from "../model/RootState";
import WcifEvent from "../model/WcifEvent";
import {
    defaultWcif,
    useCompetitionWcif,
    useSetEventExtension,
} from "../util/wcif.util";
import { useCallback, useMemo } from "react";
import {
    buildMbldExtension,
    findExtension,
    mbldCubesExtensionId,
    upsertExtension,
} from "../util/extension.util";
import { useIsFetching, useQuery } from "@tanstack/react-query";
import tnoodleApi from "../api/tnoodle.api";

interface MbldDetailProps {
    mbldWcifEvent: WcifEvent;
}

const MbldDetail = ({ mbldWcifEvent }: MbldDetailProps) => {
    const selectedCompetitionId = useSelector(
        (state: RootState) => state.informationSlice.selectedCompetitionId,
    );
    const generatingScrambles =
        useIsFetching({ queryKey: ["scramble-zip"] }) > 0;

    const { data: wcif = defaultWcif } = useCompetitionWcif(selectedCompetitionId);

    const { data: bestMbldAttempt } = useQuery({
        queryKey: ["best-mbld-attempt", wcif.persons],
        queryFn: () => tnoodleApi.fetchBestMbldAttempt(wcif.persons),
        select: (resp) => resp.data?.attempted,
        enabled: selectedCompetitionId !== undefined,
    });

    const mbld = useMemo(() => {
        return (
            findExtension(mbldWcifEvent, mbldCubesExtensionId)?.data
                ?.requestedScrambles ||
            bestMbldAttempt ||
            MBLD_DEFAULT
        );
    }, [mbldWcifEvent, bestMbldAttempt]);

    const setEventExtension = useSetEventExtension(selectedCompetitionId);

    const updateEventMbld = useCallback(
        (mbld: number) => {
            setEventExtension(
                mbldWcifEvent,
                mbldCubesExtensionId,
                () => buildMbldExtension(mbld),
                (newWcifEvent) => ({
                    ...newWcifEvent,
                    rounds: newWcifEvent.rounds.map((wcifRound) => {
                        const overrideExtension = buildMbldExtension(mbld);

                        return upsertExtension(wcifRound, overrideExtension);
                    }),
                }),
            );
        },
        [mbldWcifEvent, setEventExtension],
    );

    return (
        <tfoot>
            <tr>
                <th colSpan={3} className="text-end">
                    <label
                        className="col-form-label"
                        htmlFor="mbld-scrambles-amount"
                    >
                        Select the number of scrambles
                    </label>
                </th>
                <td>
                    <input
                        id="mbld-scrambles-amount"
                        className="form-control bg-dark text-white"
                        type="number"
                        value={mbld}
                        onChange={(e) =>
                            updateEventMbld(Number(e.target.value))
                        }
                        min={MBLD_MIN}
                        required
                        disabled={generatingScrambles}
                    />
                </td>
            </tr>
            {!!mbld && !!bestMbldAttempt && mbld < bestMbldAttempt && (
                <tr className="alert alert-warning">
                    <th colSpan={4}>
                        {`You selected ${mbld} cubes for Multi-Blind, but there's a competitor who already tried ${bestMbldAttempt} at a competition. Proceed if you are really certain of it.`}
                    </th>
                </tr>
            )}
        </tfoot>
    );
};

export default MbldDetail;

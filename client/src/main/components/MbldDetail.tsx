import { useDispatch, useSelector } from "react-redux";
import { MBLD_DEFAULT, MBLD_MIN } from "../constants/wca.constants";
import type RootState from "../model/RootState";
import { setWcifEvent } from "../redux/slice/WcifSlice";
import { setFileZip } from "../redux/slice/ScramblingSlice";
import type { WcifEvent } from "../api/wca.schemas";
import {
    buildMbldExtension,
    getBestMbldAttemptKey,
    mbldCubesExtensionId,
} from "../util/wcif.util";
import { useCallback, useMemo } from "react";
import {
    findExtension,
    setExtensionLazily,
    upsertExtension,
} from "../util/extension.util";
import { useTNoodleApi } from "../api/client.ts";
import { useQuery } from "@tanstack/react-query";

interface MbldDetailProps {
    mbldWcifEvent: WcifEvent;
}

const MbldDetail = ({ mbldWcifEvent }: MbldDetailProps) => {
    const wcif = useSelector((state: RootState) => state.wcifSlice.wcif);
    const generatingScrambles = useSelector(
        (state: RootState) => state.scramblingSlice.generatingScrambles,
    );

    const tnoodleApiClient = useTNoodleApi();

    const { data: bestMbldAttempt } = useQuery({
        queryKey: getBestMbldAttemptKey(wcif.persons),
        queryFn: (options) =>
            tnoodleApiClient.calculateMbldBest(
                { body: wcif.persons },
                options,
            ),
        select: (data) => data.attempted,
    });

    const mbld = useMemo(() => {
        return (
            findExtension(mbldWcifEvent, mbldCubesExtensionId)?.data
                ?.requestedScrambles ||
            bestMbldAttempt ||
            MBLD_DEFAULT
        );
    }, [mbldWcifEvent, bestMbldAttempt]);

    const dispatch = useDispatch();

    const updateEventMbld = useCallback(
        (mbld: number) => {
            const updatedEvent = setExtensionLazily(
                mbldWcifEvent,
                mbldCubesExtensionId,
                () => buildMbldExtension(mbld),
            );

            const updatedWithRounds = {
                ...updatedEvent,
                rounds: updatedEvent.rounds.map((wcifRound) => {
                    const overrideExtension = buildMbldExtension(mbld);

                    return upsertExtension(wcifRound, overrideExtension);
                }),
            };

            dispatch(setWcifEvent(updatedWithRounds));
            dispatch(setFileZip());
        },
        [dispatch, mbldWcifEvent],
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

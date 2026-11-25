import { MBLD_DEFAULT, MBLD_MIN } from "../constants/wca.constants";
import type { WcifEvent } from "../api/wca.schemas";
import {
    defaultWcif,
    getBestMbldAttemptKey,
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
import { useTNoodleApi } from "../api/client.ts";
import { useIsMutating, useQuery } from "@tanstack/react-query";

interface MbldDetailProps {
    mbldWcifEvent: WcifEvent;
}

const MbldDetail = ({ mbldWcifEvent }: MbldDetailProps) => {
    const generatingScrambles =
        useIsMutating({ mutationKey: ["scramble-zip"] }) > 0;

    const { data: wcif = defaultWcif } = useCompetitionWcif();

    const tnoodleApiClient = useTNoodleApi();

    const { data: bestMbldAttempt } = useQuery({
        queryKey: getBestMbldAttemptKey(wcif.persons),
        queryFn: (options) =>
            tnoodleApiClient.calculateMbldBest(
                { body: wcif.persons },
                options,
            ),
        select: (data) => data?.attempted,
    });

    const mbld = useMemo(() => {
        return (
            findExtension(mbldWcifEvent, mbldCubesExtensionId)?.data
                ?.requestedScrambles ||
            bestMbldAttempt ||
            MBLD_DEFAULT
        );
    }, [mbldWcifEvent, bestMbldAttempt]);

    const setEventExtension = useSetEventExtension(wcif);

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
        [setEventExtension, mbldWcifEvent],
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

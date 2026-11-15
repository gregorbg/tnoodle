import axios from "axios";
import type ScrambleProgram from "../model/ScrambleProgram";
import type Competition from "../model/Competition";
import type Person from "../model/Person";
import type Wcif from "../model/Wcif";
import { getWcaOrigin } from "./wca.oidc";
import type { User } from "oidc-client-ts";

// It's not pretty, but with Axios and the current client infra that's the way it has to be.
let wcaAccessToken: string | undefined = undefined;
let expiration: string | undefined = undefined;

export const setWcaAccessToken = (user: User) => {
    wcaAccessToken = user.access_token;

    const now = new Date();
    expiration = now.setSeconds(now.getSeconds() + Number(user.expires_in)) + "";
};

export function toWcaUrl(path: string) {
    return `${getWcaOrigin()}${path}`;
}

class WcaApi {
    fetchMe = () => this.wcaApiFetch<{ me: Person }>("/me");

    fetchVersionInfo = () =>
        axios.get<ScrambleProgram>(toWcaUrl("/api/v0/scramble-program"));

    getCompetitionJson = (competitionId: string, formatVersion: string) => {
        return this.wcaApiFetch<Wcif>(
            `/competitions/${competitionId}/wcif/version/${formatVersion}`,
        );
    };

    getUpcomingManageableCompetitions = () => {
        const oneWeekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
        const params = new URLSearchParams({
            managed_by_me: true.toString(),
            start: oneWeekAgo.toISOString(),
            sort: "start_date",
        });
        return this.wcaApiFetch<Competition[]>(
            `/competitions?${params.toString()}`,
        );
    };

    get isLogged() {
        if (!wcaAccessToken) {
            return false;
        }

        if (!expiration) {
            return false;
        }

        return new Date() < new Date(Number(expiration));
    };

    private wcaApiFetch<T>(path: string) {
        var baseApiUrl = toWcaUrl("/api/v0");
        let fetchOptions = {
            headers: {
                Authorization: `Bearer ${wcaAccessToken}`,
                "Content-Type": "application/json",
            },
        };

        return axios.get<T>(baseApiUrl + path, fetchOptions);
    }
}

const wcaApi = new WcaApi();
export default wcaApi;

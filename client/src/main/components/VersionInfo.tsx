import tnoodleApi from "../api/tnoodle.api";
import wcaApi from "../api/wca.api";
import { useQuery } from "@tanstack/react-query";

const VersionInfo = () => {
    // WCA API response
    const { data: wcaScrambleProgram } = useQuery({
        queryKey: ["wca-scramble-program"],
        queryFn: () => wcaApi.fetchVersionInfo(),
        select: (resp) => resp.data,
    });

    // TNoodle backend API response
    const { data: tnoodleVersion } = useQuery({
        queryKey: ["tnoodle-version"],
        queryFn: () => tnoodleApi.fetchRunningVersion(),
        select: (resp) => resp.data,
    });

    const runningVersion = `${tnoodleVersion?.projectName}-${tnoodleVersion?.projectVersion}`;

    // what we make of it
    const versionAllowed = wcaScrambleProgram?.allowed?.includes(runningVersion);
    const signatureValid = tnoodleVersion?.signedBuild && tnoodleVersion?.signatureKeyBytes === wcaScrambleProgram?.publicKeyBytes;

    // We cannot analyze TNoodle version here. We do not bother the user.
    if (wcaScrambleProgram === undefined || tnoodleVersion === undefined) {
        return null;
    }

    // Running version is not the most recent release
    if (runningVersion !== wcaScrambleProgram.current.name) {
        // Running version is allowed, but not the latest.
        if (versionAllowed) {
            return (
                <div className="alert alert-info m-0">
                    You are running {runningVersion}, which is still allowed,
                    but you should upgrade to {wcaScrambleProgram.current.name} available{" "}
                    <a href={wcaScrambleProgram.current.download}>here</a> as soon as
                    possible.
                </div>
            );
        }

        return (
            <div className="alert alert-danger m-0">
                You are running {runningVersion}, which is not allowed. Do not
                use scrambles generated in any official competition and consider
                downloading the latest version{" "}
                <a href={wcaScrambleProgram.current.download}>here</a>.
            </div>
        );
    }

    // Generated version is not an officially signed jar
    if (!signatureValid) {
        return (
            <div className="alert alert-danger m-0">
                You are running an unsigned TNoodle release. Do not use
                scrambles generated in any official competition and consider
                downloading the official program{" "}
                <a href={wcaScrambleProgram.current.download}>here</a>
            </div>
        );
    }

    return null;
};

export default VersionInfo;

import { useQuery } from "@tanstack/react-query";
import { useTNoodleQueries, useWcaQueries } from "../api/client.ts";

const VersionInfo = () => {
    const tnoodleQueries = useTNoodleQueries();
    const { data: tnoodleVersionInfo } = useQuery(
        tnoodleQueries.getVersionInfoOptions(),
    );

    const wcaQueries = useWcaQueries();
    const { data: wcaVersionInfo } = useQuery(
        wcaQueries.getScrambleProgramOptions(),
    );

    const runningVersion =
        !!tnoodleVersionInfo?.project_name &&
        !!tnoodleVersionInfo.project_version
            ? `${tnoodleVersionInfo.project_name}-${tnoodleVersionInfo.project_version}`
            : "";

    const signatureValid =
        tnoodleVersionInfo?.is_signed_build &&
        tnoodleVersionInfo?.signature_key_bytes ===
            wcaVersionInfo?.publicKeyBytes;
    const versionAllowed = wcaVersionInfo?.allowed?.includes(runningVersion);

    // We cannot analyze TNoodle version here. We do not bother the user.
    if (!tnoodleVersionInfo || !wcaVersionInfo) {
        return null;
    }

    // Running version is not the most recent release
    if (runningVersion !== wcaVersionInfo.current.name) {
        // Running version is allowed, but not the latest.
        if (versionAllowed) {
            return (
                <div className="alert alert-info m-0" role="alert">
                    You are running {runningVersion}, which is still allowed,
                    but you should upgrade to {wcaVersionInfo.current.name}{" "}
                    available <a href={wcaVersionInfo.current.download}>here</a>{" "}
                    as soon as possible.
                </div>
            );
        }

        return (
            <div className="alert alert-danger m-0" role="alert">
                You are running {runningVersion}, which is not allowed. Do not
                use scrambles generated in any official competition and consider
                downloading the latest version{" "}
                <a href={wcaVersionInfo.current.download}>here</a>.
            </div>
        );
    }

    // Generated version is not an officially signed jar
    if (!signatureValid) {
        return (
            <div className="alert alert-danger m-0" role="alert">
                You are running an unsigned TNoodle release. Do not use
                scrambles generated in any official competition and consider
                downloading the official program{" "}
                <a href={wcaVersionInfo.current.download}>here</a>
            </div>
        );
    }

    return null;
};

export default VersionInfo;

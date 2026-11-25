import { useMemo } from "react";
import {
    createClient as createTNoodleClient,
    type OperationId as TNoodleOperationId,
    type OperationPath as TNoodleOperationPath,
    OPERATIONS as TNOODLE_OPERATIONS,
    type Ops as TNoodleOps,
} from "./tnoodle.ts";
import { createQueryFactories as createTNoodleQueryFactories } from "./tnoodle.tanstack.ts";
import {
    createClient as createWcaClient,
    type OperationId as WcaOperationId,
    type OperationPath as WcaOperationPath,
    OPERATIONS as WCA_OPERATIONS,
    type Ops as WcaOps,
} from "./wca.ts";
import { createQueryFactories as createWcaQueryFactories } from "./wca.tanstack.ts";
import { getWcaOrigin } from "./wca.oidc.ts";
import { useAuth } from "react-oidc-context";

export const useTNoodleApi = () => {
    return useMemo(() => {
        return createTNoodleClient<
            TNoodleOps,
            TNoodleOperationId,
            TNoodleOperationPath
        >(TNOODLE_OPERATIONS, { serverUrl: "http://localhost:2014" });
    }, []);
};

export const useTNoodleQueries = () => {
    const tnoodleApi = useTNoodleApi();

    return useMemo(() => {
        return createTNoodleQueryFactories(tnoodleApi);
    }, [tnoodleApi]);
};

export const useWcaApi = () => {
    const auth = useAuth();

    return useMemo(() => {
        if (auth.isAuthenticated) {
            return createWcaClient<WcaOps, WcaOperationId, WcaOperationPath>(
                WCA_OPERATIONS,
                {
                    serverUrl: getWcaOrigin(),
                    auth: { bearer: auth.user?.access_token },
                },
            );
        }

        return createWcaClient<WcaOps, WcaOperationId, WcaOperationPath>(
            WCA_OPERATIONS,
            { serverUrl: getWcaOrigin() },
        );
    }, [auth.isAuthenticated, auth.user]);
};

export const useWcaQueries = () => {
    const wcaApi = useWcaApi();

    return useMemo(() => {
        return createWcaQueryFactories(wcaApi);
    }, [wcaApi]);
};

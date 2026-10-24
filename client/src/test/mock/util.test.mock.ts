import { AxiosHeaders } from "axios";
import { QueryClient } from "@tanstack/react-query";

export const axiosResponse = {
    status: 200,
    statusText: "OK",
    headers: {},
    config: {
        headers: new AxiosHeaders(),
    },
};

// Options per https://tanstack.com/query/latest/docs/framework/react/guides/testing
export const getDefaultQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
                gcTime: Infinity
            },
        },
    });

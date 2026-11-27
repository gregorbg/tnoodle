import { act, render } from "@testing-library/react";
import userEvent, { type UserEvent } from "@testing-library/user-event";
import React, { type ReactElement } from "react";
import { Provider } from "react-redux";
import { type AppStore, createStore } from "../../main/redux/Store";
import MockAuthContext from "../mock/MockAuthContext";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

interface RenderOptions {
    store?: AppStore;
    queryClient?: QueryClient;
    isAuthenticated?: boolean;
}

// Sane defaults as per https://tanstack.com/query/latest/docs/framework/react/guides/testing
export const createQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
                gcTime: Infinity,
            },
        },
    });

/**
 * Renders `ui` inside the app's providers with a fresh store,
 * and waits for the mocked requests fired on mount to settle.
 */
export const renderWithProviders = async (
    ui: ReactElement,
    {
        store = createStore(),
        queryClient = createQueryClient(),
        isAuthenticated = false,
    }: RenderOptions = {},
) => {
    const user = userEvent.setup();

    // `act` flushes the effects and the (already resolved) mocked requests
    // they trigger, so tests can also assert that something is *absent*.
    const result = await act(async () =>
        render(
            <React.StrictMode>
                <QueryClientProvider client={queryClient}>
                    <Provider store={store}>
                        <MockAuthContext isAuthenticated={isAuthenticated}>
                            {ui}
                        </MockAuthContext>
                    </Provider>
                </QueryClientProvider>
            </React.StrictMode>,
        ),
    );

    return { store, queryClient, user, ...result };
};

/** Replaces the whole content of an input, like selecting it and typing over it. */
export const replaceValue = async (
    user: UserEvent,
    input: HTMLElement,
    value: string | number,
) =>
    user.type(input, String(value), {
        initialSelectionStart: 0,
        initialSelectionEnd: (input as HTMLInputElement).value.length,
    });

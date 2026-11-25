import React from "react";
import { createRoot } from "react-dom/client";
import { Provider } from "react-redux";
import App from "./App";
import "./index.css";
import store from "./main/redux/Store";
import reportWebVitals from "./reportWebVitals";
import { AuthProvider } from "react-oidc-context";
import oidcConfig, { gotoPreLoginPath } from "./main/api/wca.oidc";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const root = createRoot(document.getElementById("root") as HTMLDivElement);

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: Infinity,
        },
    },
});

root.render(
    <QueryClientProvider client={queryClient}>
        <Provider store={store}>
            <AuthProvider {...oidcConfig} onSigninCallback={gotoPreLoginPath}>
                <App />
            </AuthProvider>
        </Provider>
    </QueryClientProvider>,
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();

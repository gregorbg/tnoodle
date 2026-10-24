import { render, act, fireEvent } from "@testing-library/react";
import React from "react";
import EntryInterface from "../main/components/EntryInterface";
import { getDefaultQueryClient } from "./mock/util.test.mock";
import { QueryClientProvider } from "@tanstack/react-query";
import { getWcifQueryKey } from "../main/util/wcif.util";
import type Wcif from "../main/model/Wcif";

let container = document.createElement("div");
beforeEach(() => {
    // setup a DOM element as a render target
    container = document.createElement("div");
    document.body.appendChild(container);
});

afterEach(() => {
    // cleanup on exiting
    container.remove();
    container = document.createElement("div");
});

it("Competition name should be already filled with current date and changes should go to the store", async () => {
    const queryClient = getDefaultQueryClient();
    const setPassword = jest.fn();

    // Render component
    await act(async () => {
        render(
            <React.StrictMode>
                <QueryClientProvider client={queryClient}>
                    <EntryInterface password="" setPassword={setPassword} />
                </QueryClientProvider>
            </React.StrictMode>,
            { container },
        );
    });

    const today = new Date().toISOString().split("T")[0];

    const input = document.getElementById(
        "competition-name",
    )! as HTMLInputElement;
    const expected = "Scrambles for " + today;
    expect(input.value).toEqual(expected);

    const typed = "Competition Name 2020";
    fireEvent.change(input, { target: { value: typed } });

    // New competition name should go to the store
    const storedWcif = queryClient.getQueryData<Wcif>(getWcifQueryKey());
    expect(storedWcif?.name).toBe(typed);
    expect(storedWcif?.shortName).toBe(typed);
});

it("Password should toggle visibility", async () => {
    const queryClient = getDefaultQueryClient();
    const setPassword = jest.fn();

    // Render component
    await act(async () => {
        render(
            <React.StrictMode>
                <QueryClientProvider client={queryClient}>
                    <EntryInterface password="123456" setPassword={setPassword} />
                </QueryClientProvider>
            </React.StrictMode>,
            { container },
        );
    });

    const input = container.querySelector("#password")! as HTMLInputElement;
    const passwordToggler = container.querySelector(".input-group-text")!;

    // Type password at first
    expect(input.type).toBe("password");

    // After the click, it should be type text
    await act(async () => {
        fireEvent.click(passwordToggler);
    });

    expect(input.type).toBe("text");
    expect(input.textContent).toBe("123456");
});

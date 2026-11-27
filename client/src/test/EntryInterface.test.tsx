import { screen } from "@testing-library/react";
import EntryInterface from "../main/components/EntryInterface";
import { setCompetitionId } from "../main/redux/ScramblerSlice.ts";
import { createStore } from "../main/redux/Store";
import { renderWithProviders, replaceValue } from "./util/render";
import type { Wcif } from "../main/api/wca.schemas.ts";
import { getWcifQueryKey } from "../main/api/wca.tanstack.ts";

describe("competition name", () => {
    it("defaults to today's date", async () => {
        await renderWithProviders(<EntryInterface />);

        const today = new Date().toISOString().split("T")[0];
        expect(screen.getByLabelText("Competition Name")).toHaveValue(
            `Scrambles for ${today}`,
        );
    });

    it("renames the competition when edited", async () => {
        const { queryClient, user } = await renderWithProviders(<EntryInterface />);

        await replaceValue(
            user,
            screen.getByLabelText("Competition Name"),
            "Competition Name 2020",
        );

        const wcif = queryClient.getQueryData<Wcif>(getWcifQueryKey());
        expect(wcif).not.toBeUndefined();

        expect(wcif!.name).toBe("Competition Name 2020");
        expect(wcif!.shortName).toBe("Competition Name 2020");
    });

    it("cannot be edited for a competition selected from the WCA", async () => {
        const store = createStore();
        store.dispatch(setCompetitionId("AmazingCompetition2026"));

        await renderWithProviders(<EntryInterface />, { store });

        expect(screen.getByLabelText("Competition Name")).toBeDisabled();
    });
});

describe("password", () => {
    it("is hidden until the visibility toggle is clicked", async () => {
        const { user } = await renderWithProviders(<EntryInterface />);
        const password = screen.getByLabelText("Password");

        expect(password).toHaveAttribute("type", "password");

        await user.click(screen.getByRole("button", { name: "Show password" }));
        expect(password).not.toHaveAttribute("type", "password");

        await user.click(screen.getByRole("button", { name: "Hide password" }));
        expect(password).toHaveAttribute("type", "password");
    });

    it("is stored as it is typed", async () => {
        const { store, user } = await renderWithProviders(<EntryInterface />);

        expect(store.getState().scramblerSlice.password).toBe("");

        await user.type(screen.getByLabelText("Password"), "123456");

        expect(store.getState().scramblerSlice.password).toBe("123456");
    });
});

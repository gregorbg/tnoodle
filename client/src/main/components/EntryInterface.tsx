import { useState } from "react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { useDispatch, useSelector } from "react-redux";
import type RootState from "../model/RootState";
import { setPassword } from "../redux/ScramblerSlice.ts";
import { defaultWcif } from "../util/wcif.util.ts";
import {
    useCompetitionWcif,
    useIsGeneratingScrambles,
    useIsManualGeneration,
    useSetCompetitionName,
} from "../api/queries.ts";

const EntryInterface = () => {
    const [showPassword, setShowPassword] = useState(false);

    const password = useSelector(
        (state: RootState) => state.scramblerSlice.password,
    );

    const generatingScrambles = useIsGeneratingScrambles();
    const isManualGeneration = useIsManualGeneration();

    const { data: wcif = defaultWcif } = useCompetitionWcif();

    const dispatch = useDispatch();
    const setCompetitionName = useSetCompetitionName(wcif);

    const handlePasswordChange = (password: string) =>
        dispatch(setPassword(password));

    return (
        <>
            <div className="col-sm-4 text-start">
                <label
                    className="form-label fw-bold"
                    htmlFor="competition-name"
                >
                    Competition Name
                </label>
                <input
                    id="competition-name"
                    className="form-control"
                    placeholder="Competition Name"
                    onChange={(e) => setCompetitionName(e.target.value)}
                    value={wcif.name}
                    disabled={!isManualGeneration || generatingScrambles}
                    required
                />
            </div>

            <div className="col-sm-4 text-start">
                <label className="form-label fw-bold" htmlFor="password">
                    Password
                </label>
                <div className="input-group">
                    <input
                        id="password"
                        className="form-control"
                        placeholder="Password"
                        type={showPassword ? "" : "password"}
                        onChange={(e) => handlePasswordChange(e.target.value)}
                        value={password}
                        disabled={generatingScrambles}
                    />
                    <button
                        type="button"
                        className="input-group-text"
                        onClick={() => setShowPassword(!showPassword)}
                        aria-label={
                            showPassword ? "Hide password" : "Show password"
                        }
                    >
                        {showPassword ? <FaEye /> : <FaEyeSlash />}
                    </button>
                </div>
            </div>
        </>
    );
};

export default EntryInterface;

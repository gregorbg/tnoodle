import { useState } from "react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { useDispatch, useSelector } from "react-redux";
import type RootState from "../model/RootState";
import { setFileZip, setPassword } from "../redux/slice/ScramblingSlice";
import { useSetCompetitionName } from "../util/wcif.util.ts";

const EntryInterface = () => {
    const [showPassword, setShowPassword] = useState(false);

    const wcif = useSelector((state: RootState) => state.wcifSlice.wcif);
    const isManualSelection = useSelector(
        (state: RootState) => state.scramblingSlice.isManualSelection,
    );
    const password = useSelector(
        (state: RootState) => state.scramblingSlice.password,
    );
    const competitionName = useSelector(
        (state: RootState) => state.wcifSlice.wcif.name,
    );
    const generatingScrambles = useSelector(
        (state: RootState) => state.scramblingSlice.generatingScrambles,
    );

    const dispatch = useDispatch();
    const setCompetitionName = useSetCompetitionName(wcif.id);

    const handlePasswordChange = (password: string) => {
        dispatch(setPassword(password));

        // Require another zip with the new password, in case there was a zip generated.
        dispatch(setFileZip());
    };

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
                    onChange={(e) =>
                        setCompetitionName(e.target.value)
                    }
                    value={competitionName}
                    disabled={!isManualSelection || generatingScrambles}
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

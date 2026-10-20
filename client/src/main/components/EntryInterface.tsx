import { useState } from "react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { useDispatch, useSelector } from "react-redux";
import RootState from "../model/RootState";
import { setFileZip, setPassword } from "../redux/slice/ScramblingSlice";
import { useCompetitionWcif, useSetCompetitionName } from "../util/wcif.util";

const EntryInterface = () => {
    const [showPassword, setShowPassword] = useState(false);

    const isManualSelection = useSelector(
        (state: RootState) => state.informationSlice.isManualSelection,
    );
    const password = useSelector(
        (state: RootState) => state.scramblingSlice.password,
    );
    const selectedCompetitionId = useSelector(
        (state: RootState) => state.informationSlice.selectedCompetitionId,
    );
    const generatingScrambles = useSelector(
        (state: RootState) => state.scramblingSlice.generatingScrambles,
    );

    const { data: wcif } = useCompetitionWcif(selectedCompetitionId);

    const dispatch = useDispatch();
    const setCompetitionName = useSetCompetitionName(selectedCompetitionId);

    const handleCompetitionNameChange = (name: string) => {
        setCompetitionName(name);

        // Require another zip with the new name.
        dispatch(setFileZip());
    };

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
                        handleCompetitionNameChange(e.target.value)
                    }
                    value={wcif.name}
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
                    <span
                        className="input-group-text"
                        onClick={() => setShowPassword(!showPassword)}
                    >
                        {showPassword ? <FaEye /> : <FaEyeSlash />}
                    </span>
                </div>
            </div>
        </>
    );
};

export default EntryInterface;

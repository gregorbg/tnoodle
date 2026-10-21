import { useState } from "react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import {
    defaultWcif,
    useCompetitionWcif,
    useSetCompetitionName,
} from "../util/wcif.util";
import { useIsMutating } from "@tanstack/react-query";

const EntryInterface = ({
    competitionId,
    password,
    setPassword,
}: {
    competitionId?: string;
    password: string;
    setPassword: (password: string) => void;
}) => {
    const [showPassword, setShowPassword] = useState(false);

    const generatingScrambles =
        useIsMutating({ mutationKey: ["scramble-zip"] }) > 0;

    const { data: wcif = defaultWcif } = useCompetitionWcif(
        competitionId,
    );

    const setCompetitionName = useSetCompetitionName(competitionId);

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
                    disabled={
                        competitionId !== undefined ||
                        generatingScrambles
                    }
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
                        onChange={(e) => setPassword(e.target.value)}
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

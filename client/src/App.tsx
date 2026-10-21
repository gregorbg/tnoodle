import "bootstrap/dist/css/bootstrap.min.css";
import "./App.css";
import Main from "./main/components/Main";
import SideBar from "./main/components/SideBar";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

const queryClient = new QueryClient();

const App = () => {
    const [competitionId, setCompetitionId] = useState<string>();
    const [showColorPicker, setShowColorPicker] = useState(false);

    return (
        <QueryClientProvider client={queryClient}>
            <div className="App container-fluid">
                <div className="row">
                    <div
                        className="col-lg-3 bg-dark overflow-auto"
                        id="side-bar"
                    >
                        <SideBar competitionId={competitionId} setCompetitionId={setCompetitionId} showColorPicker={showColorPicker} setShowColorPicker={setShowColorPicker} />
                    </div>
                    <div className="col-lg-9 m-0 p-0">
                        <Main competitionId={competitionId} showColorPicker={showColorPicker} />
                    </div>
                </div>
            </div>
        </QueryClientProvider>
    );
};

export default App;

export const BASE_PATH = process.env.PUBLIC_URL;

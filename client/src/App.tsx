import "bootstrap/dist/css/bootstrap.min.css";
import "./App.css";
import Main from "./main/components/Main";
import SideBar from "./main/components/SideBar";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const queryClient = new QueryClient();

const App = () => (
    <QueryClientProvider client={queryClient}>
        <div className="App container-fluid">
            <div className="row">
                <div className="col-lg-3 bg-dark overflow-auto" id="side-bar">
                    <SideBar />
                </div>
                <div className="col-lg-9 m-0 p-0">
                    <Main />
                </div>
            </div>
        </div>
    </QueryClientProvider>
);

export default App;

export const BASE_PATH = process.env.PUBLIC_URL;

import React, { Component } from "react";
import Axios, { AxiosError } from "axios";
import "./Interceptor.css";

interface InterceptorState {
    message: string;
    stackTrace: string;
    showMore: boolean;
    showThis: boolean;
}

class Interceptor extends Component<{}, InterceptorState> {
    messageDurationInSeconds = 10;

    constructor(props: object) {
        super(props);

        Axios.interceptors.request.use((config) => {
            // TODO set loading
            return config;
        });

        Axios.interceptors.response.use(undefined, (error) => {
            this.handleHttpError(error);
            return Promise.reject(error);
        });

        this.state = {
            message: "",
            stackTrace: "",
            showMore: false,
            showThis: false,
        };
    }

    handleHttpError = (error: AxiosError) => {
        try {
            this.updateMessage(error.response?.data);
        } catch (e) {
            this.updateMessage(e);
        }
    };

    updateMessage = (data: any) => {
        // Clear the message after some seconds
        let message = data?.message || data?.statusText || JSON.stringify(data);
        let stackTrace = data?.stackTrace ?? "";

        setTimeout(() => {
            // We only clear the message if the user did not click "Show more"
            if (!this.state.showThis) {
                this.clear();
            }
        }, 1000 * this.messageDurationInSeconds);

        this.setState({ ...this.state, message, stackTrace });
    };

    // If the user clicks show more, message will be there until close.
    setShowMore = () =>
        this.setState({ ...this.state, showMore: true, showThis: true });

    showMore = () => {
        if (!this.state.stackTrace) {
            return null;
        }
        if (this.state.showMore) {
            return (
                <textarea
                    className="form-control"
                    value={this.state.stackTrace}
                    rows={10}
                    disabled
                />
            );
        }
        return (
            <p className="text-right">
                <button className="btn btn-primary" onClick={this.setShowMore}>
                    Show more
                </button>
            </p>
        );
    };

    clear = () => {
        this.setState({
            ...this.state,
            message: "",
            stackTrace: "",
            showMore: false,
        });
    };

    render = () => {
        if (!this.state.message) {
            return null;
        }
        return (
            <div className="container-fluid">
                <div className="row">
                    <div className={"col-12 alert alert-danger"}>
                        <p>
                            {this.state.message}
                            <button
                                type="button"
                                className="close"
                                data-dismiss="modal"
                                aria-label="Close"
                                onClick={this.clear}
                            >
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </p>
                        {this.showMore()}
                    </div>
                </div>
            </div>
        );
    };
}

export default Interceptor;

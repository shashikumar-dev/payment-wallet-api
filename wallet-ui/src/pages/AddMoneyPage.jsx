import { useState } from "react";
import { useNavigate } from "react-router-dom";

import apiClient from "../api/apiClient";
import "./WalletActionPage.css";

function AddMoneyPage() {
    const navigate = useNavigate();

    const [amount, setAmount] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();

        setError("");

        const numericAmount = Number(amount);

        if (
            !Number.isFinite(numericAmount) ||
            numericAmount <= 0
        ) {
            setError("Please enter a valid amount.");
            return;
        }

        setLoading(true);

        try {
            const response = await apiClient.post(
                "/wallets/add-money",
                {
                    amount: numericAmount,
                }
            );

            console.log(
                "Add money response:",
                response.data
            );

            navigate("/dashboard", {
                replace: true,
            });
        } catch (err) {
            console.error(
                "Add money error:",
                err.response?.data || err
            );

            if (err.response?.status === 429) {
                setError(
                    "Too many requests. Please wait a moment and try again."
                );
            } else if (err.response?.status === 401) {
                setError(
                    "Your session has expired. Please sign in again."
                );
            } else {
                setError(
                    err.response?.data?.message ||
                    "Unable to add money. Please try again."
                );
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="action-page">
            <div className="action-page-card">

                <button
                    className="back-button"
                    onClick={() => navigate("/dashboard")}
                >
                    ← Back to dashboard
                </button>

                <div className="action-page-header">
                    <div className="large-action-icon">
                        +
                    </div>

                    <div>
                        <p className="action-label">
                            WALLET
                        </p>

                        <h1>Add money</h1>

                        <p>
                            Add funds to your WalletPay balance.
                        </p>
                    </div>
                </div>

                {error && (
                    <div className="error-message">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>

                    <div className="amount-field">
                        <label htmlFor="amount">
                            Amount
                        </label>

                        <div className="money-input">
                            <span>₹</span>

                            <input
                                id="amount"
                                type="number"
                                min="0.01"
                                step="0.01"
                                placeholder="0.00"
                                value={amount}
                                onChange={(event) => {
                                    setAmount(event.target.value);

                                    if (error) {
                                        setError("");
                                    }
                                }}
                                required
                                disabled={loading}
                            />
                        </div>
                    </div>

                    <div className="quick-amounts">
                        {[500, 1000, 2000, 5000].map(
                            (value) => (
                                <button
                                    key={value}
                                    type="button"
                                    onClick={() =>
                                        setAmount(String(value))
                                    }
                                >
                                    ₹{value.toLocaleString("en-IN")}
                                </button>
                            )
                        )}
                    </div>

                    <div className="info-box">
                        <span>ⓘ</span>

                        <p>
                            This is a demo wallet. Adding money
                            increases the wallet balance directly;
                            no real payment is processed.
                        </p>
                    </div>

                    <button
                        className="wallet-primary-button"
                        type="submit"
                        disabled={loading}
                    >
                        {loading
                            ? "Adding money..."
                            : "Add money"}
                    </button>

                </form>

            </div>
        </div>
    );
}

export default AddMoneyPage;
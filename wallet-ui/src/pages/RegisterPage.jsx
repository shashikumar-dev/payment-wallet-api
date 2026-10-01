import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import apiClient from "../api/apiClient";

import "./LoginPage.css";

function RegisterPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    walletPayId: "",
    password: "",
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((previous) => ({
      ...previous,
      [name]: value,
    }));

    if (error) {
      setError("");
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");

    const name = formData.name.trim();

    const email = formData.email
      .trim()
      .toLowerCase();

    /*
     * User enters:
     *
     * abc
     *
     * Backend converts it to:
     *
     * abc@walletpay
     */
    const walletPayId = formData.walletPayId
      .trim()
      .toLowerCase()
      .replace(/@walletpay$/, "");

    if (!name) {
      setError("Please enter your name.");
      return;
    }

    if (!email) {
      setError("Please enter your email address.");
      return;
    }

    if (!walletPayId) {
      setError("Please choose a WalletPay ID.");
      return;
    }

    if (!/^[a-z0-9._-]+$/.test(walletPayId)) {
      setError(
        "WalletPay ID can contain letters, numbers, dots, underscores and hyphens."
      );
      return;
    }

    if (!formData.password) {
      setError("Please enter a password.");
      return;
    }

    setLoading(true);

    try {
      await apiClient.post(
        "/auth/register",
        {
          name,
          email,
          walletPayId,
          password: formData.password,
        }
      );

      navigate("/login", {
        state: {
          message:
            "Account created successfully. Please sign in.",
        },
      });
    } catch (err) {
      const message =
        err.response?.data?.message ||
        "Unable to create account. Please try again.";

      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">

      <div className="auth-brand">

        <div className="brand-content">

          <div className="brand-logo">
            W
          </div>

          <h1>WalletPay</h1>

          <p>
            Create your wallet and start making
            secure payments.
          </p>

          <div className="brand-feature">
            <span>✓</span>
            Your own WalletPay ID
          </div>

          <div className="brand-feature">
            <span>✓</span>
            Secure wallet access
          </div>

          <div className="brand-feature">
            <span>✓</span>
            Simple money transfers
          </div>

        </div>

      </div>

      <div className="auth-section">

        <div className="auth-card">

          <div className="mobile-logo">
            W
          </div>

          <p className="auth-label">
            PAYMENT WALLET
          </p>

          <h2>Create account</h2>

          <p className="auth-subtitle">
            Create your account and choose your
            unique WalletPay ID.
          </p>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>

            <div className="form-group">

              <label htmlFor="name">
                Full name
              </label>

              <input
                id="name"
                name="name"
                type="text"
                placeholder="Enter your full name"
                value={formData.name}
                onChange={handleChange}
                required
                autoComplete="name"
                disabled={loading}
              />

            </div>

            <div className="form-group">

              <label htmlFor="email">
                Email address
              </label>

              <input
                id="email"
                name="email"
                type="email"
                placeholder="you@example.com"
                value={formData.email}
                onChange={handleChange}
                required
                autoComplete="email"
                disabled={loading}
              />

            </div>

            <div className="form-group">

              <label htmlFor="walletPayId">
                WalletPay ID
              </label>

              <div className="walletpay-input">

                <input
                  id="walletPayId"
                  name="walletPayId"
                  type="text"
                  placeholder="Enter your username"
                  value={formData.walletPayId}
                  onChange={handleChange}
                  required
                  autoComplete="off"
                  pattern="[A-Za-z0-9._-]+"
                  title="Use letters, numbers, dots, underscores or hyphens"
                  disabled={loading}
                />

                <span>
                  @walletpay
                </span>

              </div>

              <small className="walletpay-hint">
                People will use this ID to send
                money to you.
              </small>

            </div>

            <div className="form-group">

              <label htmlFor="password">
                Password
              </label>

              <input
                id="password"
                name="password"
                type="password"
                placeholder="Minimum 8 characters"
                value={formData.password}
                onChange={handleChange}
                required
                minLength={8}
                autoComplete="new-password"
                disabled={loading}
              />

            </div>

            <button
              className="primary-button"
              type="submit"
              disabled={loading}
            >
              {loading
                ? "Creating account..."
                : "Create account"}
            </button>

          </form>

          <p className="auth-footer">
            Already have an account?{" "}

            <Link to="/login">
              Sign in
            </Link>
          </p>

        </div>

      </div>

    </div>
  );
}

export default RegisterPage;
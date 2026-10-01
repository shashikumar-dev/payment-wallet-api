import { useState } from "react";
import {
  Link,
  useLocation,
  useNavigate,
} from "react-router-dom";

import apiClient from "../api/apiClient";
import { useAuth } from "../context/AuthContext";

import "./LoginPage.css";

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const { login } = useAuth();

  // ---------------------------------------------------------
  // Form State
  // ---------------------------------------------------------

  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // Message passed from RegisterPage after successful signup
  const successMessage = location.state?.message;

  // ---------------------------------------------------------
  // Handle Input Changes
  // ---------------------------------------------------------

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((previous) => ({
      ...previous,
      [name]: value,
    }));

    // Clear previous login error when user starts editing
    if (error) {
      setError("");
    }
  };

  // ---------------------------------------------------------
  // Handle Login
  // ---------------------------------------------------------

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");
    setLoading(true);

    try {
      /*
       * Request flow:
       *
       * React
       *   ↓
       * API Gateway :8080
       *   ↓
       * User Service :8081
       *   ↓
       * POST /auth/login
       */
      const response = await apiClient.post(
        "/auth/login",
        {
          email: formData.email.trim(),
          password: formData.password,
        }
      );

      /*
       * Expected response:
       *
       * {
       *   accessToken: "...",
       *   tokenType: "Bearer",
       *   expiresIn: 3600
       * }
       */
      const accessToken = response.data.accessToken;

      if (!accessToken) {
        throw new Error(
          "Access token was not returned by the server."
        );
      }

      /*
       * AuthContext stores the token and updates
       * authentication state.
       */
      login(accessToken);

      /*
       * Login successful.
       */
      navigate("/dashboard", {
        replace: true,
      });
    } catch (err) {
      /*
       * Backend errors normally look like:
       *
       * {
       *   timestamp: "...",
       *   status: 401,
       *   error: "Unauthorized",
       *   message: "Invalid email or password",
       *   path: "/auth/login"
       * }
       */

      if (err.response) {
        setError(
          err.response.data?.message ||
            "Invalid email or password."
        );
      } else if (err.request) {
        /*
         * Request was sent but no response was received.
         *
         * Possible reasons:
         * - Gateway isn't running
         * - Backend isn't reachable
         * - CORS/network problem
         */
        setError(
          "Unable to connect to the server. Please try again."
        );
      } else {
        setError(
          err.message ||
            "Unable to login. Please try again."
        );
      }
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------
  // UI
  // ---------------------------------------------------------

  return (
    <div className="auth-page">

      {/* =====================================================
          LEFT SIDE
          ===================================================== */}

      <div className="auth-brand">
        <div className="brand-content">

          <div className="brand-logo">
            W
          </div>

          <h1>WalletPay</h1>

          <p>
            Simple, secure payments built with modern
            technology.
          </p>

          <div className="brand-feature">
            <span>✓</span>
            Secure money transfers
          </div>

          <div className="brand-feature">
            <span>✓</span>
            Instant wallet balance
          </div>

          <div className="brand-feature">
            <span>✓</span>
            Complete transaction history
          </div>

        </div>
      </div>

      {/* =====================================================
          RIGHT SIDE
          ===================================================== */}

      <div className="auth-section">

        <div className="auth-card">

          {/* Mobile logo */}
          <div className="mobile-logo">
            W
          </div>

          <p className="auth-label">
            PAYMENT WALLET
          </p>

          <h2>
            Welcome back
          </h2>

          <p className="auth-subtitle">
            Sign in to access your wallet.
          </p>

          {/* Registration success message */}

          {successMessage && (
            <div className="success-message">
              {successMessage}
            </div>
          )}

          {/* Login error */}

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          {/* =================================================
              LOGIN FORM
              ================================================= */}

          <form onSubmit={handleSubmit}>

            {/* Email */}

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

            {/* Password */}

            <div className="form-group">

              <label htmlFor="password">
                Password
              </label>

              <input
                id="password"
                name="password"
                type="password"
                placeholder="Enter your password"
                value={formData.password}
                onChange={handleChange}
                required
                autoComplete="current-password"
                disabled={loading}
              />

            </div>

            {/* Login button */}

            <button
              className="primary-button"
              type="submit"
              disabled={loading}
            >
              {loading
                ? "Signing in..."
                : "Sign in"}
            </button>

          </form>

          {/* Registration link */}

          <p className="auth-footer">
            Don't have an account?{" "}
            <Link to="/register">
              Create account
            </Link>
          </p>

        </div>

      </div>

    </div>
  );
}

export default LoginPage;
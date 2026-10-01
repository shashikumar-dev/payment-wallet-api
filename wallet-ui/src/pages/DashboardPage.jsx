import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import apiClient from "../api/apiClient";
import { useAuth } from "../context/AuthContext";

import "./DashboardPage.css";

function DashboardPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [user, setUser] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [recentTransactions, setRecentTransactions] =
    useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setLoading(true);
        setError("");

        /*
         * These requests are independent,
         * so execute them in parallel.
         */
        const [
          userResponse,
          walletResponse,
          transactionsResponse,
        ] = await Promise.all([
          apiClient.get("/users/me"),
          apiClient.get("/wallets/me"),
          apiClient.get(
            "/transactions",
            {
              params: {
                page: 0,
                size: 5,
              },
            }
          ),
        ]);

        setUser(userResponse.data);
        setWallet(walletResponse.data);

        const transactions =
          transactionsResponse.data?.content || [];

        setRecentTransactions(
          transactions
        );
      } catch (err) {
        if (err.response?.status === 401) {
          logout();

          navigate("/login", {
            replace: true,
          });

          return;
        }

        if (err.response?.status === 429) {
          setError(
            "Too many requests. Please wait a moment and refresh."
          );

          return;
        }

        setError(
          err.response?.data?.message ||
          "Unable to load your wallet."
        );
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, [logout, navigate]);

  const handleLogout = () => {
    logout();

    navigate("/login", {
      replace: true,
    });
  };

  const formatMoney = (amount) => {
    return new Intl.NumberFormat(
      "en-IN",
      {
        style: "currency",
        currency: "INR",
        minimumFractionDigits: 2,
      }
    ).format(Number(amount || 0));
  };

  const formatDate = (date) => {
    if (!date) {
      return "";
    }

    return new Intl.DateTimeFormat(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
      }
    ).format(new Date(date));
  };

  if (loading) {
    return (
      <div className="dashboard-loading">
        <div className="loading-spinner" />

        <p>
          Loading your wallet...
        </p>
      </div>
    );
  }

  return (
    <div className="dashboard-page">

      {/* =====================================
          SIDEBAR
          ===================================== */}

      <aside className="sidebar">

        <div className="sidebar-brand">
          <div className="sidebar-logo">
            W
          </div>

          <span>
            WalletPay
          </span>
        </div>

        <nav className="sidebar-nav">

          <button
            className="nav-item active"
            onClick={() =>
              navigate("/dashboard")
            }
          >
            <span>⌂</span>
            Dashboard
          </button>

          <button
            className="nav-item"
            onClick={() =>
              navigate("/send-money")
            }
          >
            <span>↑</span>
            Send Money
          </button>

          <button
            className="nav-item"
            onClick={() =>
              navigate("/transactions")
            }
          >
            <span>↕</span>
            Transactions
          </button>

        </nav>

        <div className="sidebar-bottom">

          <button
            className="nav-item logout-item"
            onClick={handleLogout}
          >
            <span>↪</span>
            Logout
          </button>

        </div>

      </aside>

      {/* =====================================
          MAIN CONTENT
          ===================================== */}

      <main className="dashboard-content">

        {/* HEADER */}

        <header className="dashboard-header">

          <div>

            <p className="dashboard-label">
              OVERVIEW
            </p>

            <h1>
              Welcome back
              {user?.name
                ? `, ${user.name.split(" ")[0]}`
                : ""}
            </h1>

            <p>
              Here's what's happening
              with your wallet.
            </p>

          </div>

          <div className="user-profile">

            <div className="user-avatar">
              {user?.name
                ? user.name
                  .charAt(0)
                  .toUpperCase()
                : "U"}
            </div>

            <div className="user-info">

              <strong>
                {user?.name || "User"}
              </strong>

              <span>
                {user?.walletPayId ||
                  user?.email ||
                  ""}
              </span>

            </div>

          </div>

        </header>

        {error && (
          <div className="dashboard-error">
            {error}
          </div>
        )}

        {/* =====================================
            BALANCE
            ===================================== */}

        <section className="balance-card">

          <div className="balance-card-top">

            <div>

              <p className="balance-label">
                AVAILABLE BALANCE
              </p>

              <h2>
                {formatMoney(
                  wallet?.balance
                )}
              </h2>

            </div>

            <div className="wallet-symbol">
              ₹
            </div>

          </div>

          <div className="balance-card-bottom">

            <div>

              <span>
                WalletPay ID
              </span>

              <strong>
                {user?.walletPayId ||
                  "—"}
              </strong>

            </div>

            <div>

              <span>
                Currency
              </span>

              <strong>
                {wallet?.currency ||
                  "INR"}
              </strong>

            </div>

          </div>

        </section>

        {/* =====================================
            QUICK ACTIONS
            ===================================== */}

        <section className="dashboard-section">

          <div className="section-heading">

            <div>
              <h2>
                Quick actions
              </h2>

              <p>
                Manage your wallet.
              </p>
            </div>

          </div>

          <div className="quick-actions">

            <button
              className="action-card"
              onClick={() =>
                navigate("/add-money")
              }
            >

              <div className="action-icon">
                +
              </div>

              <div>

                <strong>
                  Add Money
                </strong>

                <span>
                  Add funds to your wallet
                </span>

              </div>

              <div className="action-arrow">
                →
              </div>

            </button>

            <button
              className="action-card"
              onClick={() =>
                navigate("/send-money")
              }
            >

              <div className="action-icon">
                ↑
              </div>

              <div>

                <strong>
                  Send Money
                </strong>

                <span>
                  Pay using a WalletPay ID
                </span>

              </div>

              <div className="action-arrow">
                →
              </div>

            </button>

          </div>

        </section>

        {/* =====================================
            RECENT TRANSACTIONS
            ===================================== */}

        <section className="dashboard-section">

          <div className="section-heading">

            <div>

              <h2>
                Recent transactions
              </h2>

              <p>
                Your latest wallet activity.
              </p>

            </div>

            {recentTransactions.length > 0 && (
              <button
                className="text-button"
                onClick={() =>
                  navigate("/transactions")
                }
              >
                View all →
              </button>
            )}

          </div>

          {recentTransactions.length === 0 ? (

            <div className="empty-transactions">

              <div className="empty-icon">
                ↕
              </div>

              <h3>
                No transactions yet
              </h3>

              <p>
                Send money to another
                WalletPay user to get started.
              </p>

              <button
                className="empty-send-button"
                onClick={() =>
                  navigate("/send-money")
                }
              >
                Send money
              </button>

            </div>

          ) : (

            <div className="dashboard-transactions">

              {recentTransactions.map(
                (transaction) => {

                  const isSent =
                    transaction.type ===
                    "SENT";

                  const counterparty =
                    transaction.counterparty;

                  return (
                    <button
                      key={
                        transaction.transferId
                      }
                      className="dashboard-transaction-row"
                      onClick={() =>
                        navigate(
                          `/transactions/${transaction.transferId}`
                        )
                      }
                    >

                      <div
                        className={
                          isSent
                            ? "dashboard-transaction-icon sent"
                            : "dashboard-transaction-icon received"
                        }
                      >
                        {isSent
                          ? "↑"
                          : "↓"}
                      </div>

                      <div className="dashboard-transaction-person">

                        <strong>
                          {counterparty?.name ||
                            (isSent
                              ? "Money sent"
                              : "Money received")}
                        </strong>

                        <span className="dashboard-walletpay-id">
                          {counterparty?.walletPayId ||
                            "WalletPay user"}
                        </span>

                        <span className="dashboard-transaction-date">
                          {formatDate(
                            transaction.createdAt
                          )}
                        </span>

                      </div>

                      <div className="dashboard-transaction-right">

                        <strong
                          className={
                            isSent
                              ? "dashboard-amount sent"
                              : "dashboard-amount received"
                          }
                        >
                          {isSent
                            ? "-"
                            : "+"}

                          {formatMoney(
                            transaction.amount
                          )}
                        </strong>

                        <span className="dashboard-status">
                          {
                            transaction.status
                          }
                        </span>

                      </div>

                      <span className="dashboard-transaction-arrow">
                        ›
                      </span>

                    </button>
                  );
                }
              )}

            </div>

          )}

        </section>

      </main>

    </div>
  );
}

export default DashboardPage;
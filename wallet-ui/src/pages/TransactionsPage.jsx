import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import apiClient from "../api/apiClient";
import { useAuth } from "../context/AuthContext";

import "./TransactionsPage.css";

function TransactionsPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [transactions, setTransactions] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [page, setPage] =
    useState(0);

  const [pageInfo, setPageInfo] =
    useState({
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

  useEffect(() => {
    const loadTransactions = async () => {
      try {
        setLoading(true);
        setError("");

        const response =
          await apiClient.get(
            "/transactions",
            {
              params: {
                page,
                size: 20,
              },
            }
          );

        const data =
          response.data;

        setTransactions(
          data.content || []
        );

        setPageInfo({
          page: data.page,
          size: data.size,
          totalElements:
            data.totalElements,
          totalPages:
            data.totalPages,
          first: data.first,
          last: data.last,
        });

      } catch (err) {
        if (
          err.response?.status === 401
        ) {
          logout();

          navigate(
            "/login",
            {
              replace: true,
            }
          );

          return;
        }

        if (
          err.response?.status === 429
        ) {
          setError(
            "Too many requests. Please wait a moment and try again."
          );

          return;
        }

        setError(
          err.response?.data?.message ||
          "Unable to load transactions."
        );

      } finally {
        setLoading(false);
      }
    };

    loadTransactions();

  }, [logout, navigate, page]);

  const formatMoney = (amount) => {
    return new Intl.NumberFormat(
      "en-IN",
      {
        style: "currency",
        currency: "INR",
        minimumFractionDigits: 2,
      }
    ).format(
      Number(amount || 0)
    );
  };

  const formatDate = (date) => {
    if (!date) {
      return "";
    }

    return new Intl.DateTimeFormat(
      "en-IN",
      {
        dateStyle: "medium",
        timeStyle: "short",
      }
    ).format(
      new Date(date)
    );
  };

  if (loading) {
    return (
      <div className="transactions-loading">

        <div className="loading-spinner" />

        <p>
          Loading transactions...
        </p>

      </div>
    );
  }

  return (
    <div className="transactions-page">

      <div className="transactions-container">

        <div className="transactions-header">

          <div>

            <button
              className="back-button"
              onClick={() =>
                navigate(
                  "/dashboard"
                )
              }
            >
              ← Back to dashboard
            </button>

            <p className="transactions-label">
              WALLET ACTIVITY
            </p>

            <h1>
              Transaction history
            </h1>

            <p className="transactions-subtitle">
              View all money sent and received
              through your wallet.
            </p>

          </div>

          <button
            className="send-money-header-button"
            onClick={() =>
              navigate(
                "/send-money"
              )
            }
          >
            + Send money
          </button>

        </div>

        {error && (
          <div className="transactions-error">
            {error}
          </div>
        )}

        {!error &&
          transactions.length === 0 && (

            <div className="transactions-empty">

              <div className="empty-transaction-icon">
                ↕
              </div>

              <h2>
                No transactions yet
              </h2>

              <p>
                Your sent and received payments
                will appear here.
              </p>

              <button
                onClick={() =>
                  navigate(
                    "/send-money"
                  )
                }
              >
                Send your first payment
              </button>

            </div>
          )}

        {transactions.length > 0 && (

          <div className="transactions-card">

            <div className="transactions-card-header">

              <div>

                <strong>
                  All transactions
                </strong>

                <span>
                  {pageInfo.totalElements}{" "}
                  {pageInfo.totalElements === 1
                    ? "transaction"
                    : "transactions"}
                </span>

              </div>

            </div>

            <div className="transaction-list">

              {transactions.map(
                (transaction) => {

                  const isSent =
                    transaction.type ===
                    "SENT";

                  const counterparty =
                    transaction.counterparty;

                  return (
                    <button
                      className="transaction-row"
                      key={
                        transaction.transferId
                      }
                      onClick={() =>
                        navigate(
                          `/transactions/${transaction.transferId}`
                        )
                      }
                    >

                      <div
                        className={
                          isSent
                            ? "transaction-icon sent"
                            : "transaction-icon received"
                        }
                      >
                        {isSent
                          ? "↑"
                          : "↓"}
                      </div>

                      <div className="transaction-main">

                        <strong>
                          {counterparty?.name ||
                            (isSent
                              ? "Money sent"
                              : "Money received")}
                        </strong>

                        <span className="transaction-walletpay-id">
                          {counterparty?.walletPayId ||
                            "WalletPay user"}
                        </span>

                        <span>
                          {isSent
                            ? "Sent"
                            : "Received"}
                          {" • "}
                          {formatDate(
                            transaction.createdAt
                          )}
                        </span>

                      </div>

                      <div className="transaction-status">

                        <span
                          className={
                            transaction.status ===
                              "SUCCESS"
                              ? "status-badge success"
                              : "status-badge"
                          }
                        >
                          {
                            transaction.status
                          }
                        </span>

                      </div>

                      <div
                        className={
                          isSent
                            ? "transaction-amount sent-amount"
                            : "transaction-amount received-amount"
                        }
                      >
                        {isSent
                          ? "-"
                          : "+"}

                        {formatMoney(
                          transaction.amount
                        )}
                      </div>

                      <div className="transaction-chevron">
                        ›
                      </div>

                    </button>
                  );
                }
              )}

            </div>

            {pageInfo.totalPages > 1 && (
              <div className="pagination">

                <button
                  className="pagination-button"
                  disabled={pageInfo.first}
                  onClick={() =>
                    setPage(
                      (currentPage) =>
                        Math.max(
                          currentPage - 1,
                          0
                        )
                    )
                  }
                >
                  ← Previous
                </button>

                <div className="pagination-info">

                  <span>
                    Page
                  </span>

                  <strong>
                    {pageInfo.page + 1}
                  </strong>

                  <span>
                    of
                  </span>

                  <strong>
                    {pageInfo.totalPages}
                  </strong>

                </div>

                <button
                  className="pagination-button"
                  disabled={pageInfo.last}
                  onClick={() =>
                    setPage(
                      (currentPage) =>
                        currentPage + 1
                    )
                  }
                >
                  Next →
                </button>

              </div>
            )}

          </div>
        )}

      </div>

    </div>
  );
}

export default TransactionsPage;
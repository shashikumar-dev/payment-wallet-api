import { useEffect, useState } from "react";
import {
  useNavigate,
  useParams,
} from "react-router-dom";

import apiClient from "../api/apiClient";

import "./TransactionDetailsPage.css";

function TransactionDetailsPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [transaction, setTransaction] =
    useState(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {

    const loadTransaction =
      async () => {

        try {
          setLoading(true);
          setError("");

          const response =
            await apiClient.get(
              `/transactions/${id}`
            );

          setTransaction(
            response.data
          );

        } catch (err) {

          if (
            err.response?.status ===
            404
          ) {
            setError(
              "Transaction not found."
            );

          } else if (
            err.response?.status ===
            401
          ) {
            setError(
              "Your session has expired. Please sign in again."
            );

          } else {
            setError(
              err.response?.data
                ?.message ||
                "Unable to load transaction."
            );
          }

        } finally {
          setLoading(false);
        }
      };

    loadTransaction();

  }, [id]);

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
      return "—";
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
      <div className="transaction-details-page">

        <div className="transaction-details-card">

          <div className="transaction-details-loading">

            <div className="details-spinner" />

            <p>
              Loading transaction...
            </p>

          </div>

        </div>

      </div>
    );
  }

  if (error) {
    return (
      <div className="transaction-details-page">

        <div className="transaction-details-card error-state">

          <div className="details-error-icon">
            !
          </div>

          <h2>
            Unable to load transaction
          </h2>

          <p>
            {error}
          </p>

          <button
            className="details-primary-button"
            onClick={() =>
              navigate(
                "/transactions"
              )
            }
          >
            Back to transactions
          </button>

        </div>

      </div>
    );
  }

  if (!transaction) {
    return null;
  }

  const isSent =
    transaction.type === "SENT";

  const counterparty =
    transaction.counterparty;

  return (
    <div className="transaction-details-page">

      <div className="transaction-details-card">

        <button
          className="details-back-button"
          onClick={() =>
            navigate(
              "/transactions"
            )
          }
        >
          ← Back to transactions
        </button>

        <div className="transaction-status-section">

          <div
            className={
              isSent
                ? "transaction-type-icon sent"
                : "transaction-type-icon received"
            }
          >
            {isSent
              ? "↑"
              : "↓"}
          </div>

          <p className="details-label">
            {isSent
              ? "MONEY SENT"
              : "MONEY RECEIVED"}
          </p>

          <h1>
            {formatMoney(
              transaction.amount
            )}
          </h1>

          <div
            className={`details-status ${
              transaction.status ===
              "SUCCESS"
                ? "success"
                : ""
            }`}
          >
            <span>
              ✓
            </span>

            {transaction.status}
          </div>

        </div>

        <div className="counterparty-section">

          <p className="counterparty-label">
            {isSent
              ? "SENT TO"
              : "RECEIVED FROM"}
          </p>

          <div className="counterparty-card">

            <div className="counterparty-avatar">
              {counterparty?.name
                ?.charAt(0)
                ?.toUpperCase() ||
                "U"}
            </div>

            <div className="counterparty-info">

              <strong>
                {counterparty?.name ||
                  "WalletPay user"}
              </strong>

              <span>
                {counterparty
                  ?.walletPayId ||
                  "—"}
              </span>

            </div>

            <div className="counterparty-check">
              ✓
            </div>

          </div>

        </div>

        <div className="transaction-information">

          <h2>
            Transaction details
          </h2>

          <div className="details-row">

            <span>
              Transfer ID
            </span>

            <strong>
              #
              {
                transaction.transferId
              }
            </strong>

          </div>

          <div className="details-row">

            <span>
              Type
            </span>

            <strong
              className={
                isSent
                  ? "sent-text"
                  : "received-text"
              }
            >
              {transaction.type}
            </strong>

          </div>

          <div className="details-row">

            <span>
              Amount
            </span>

            <strong>
              {formatMoney(
                transaction.amount
              )}
            </strong>

          </div>

          <div className="details-row">

            <span>
              Status
            </span>

            <strong className="success-text">
              {transaction.status}
            </strong>

          </div>

          <div className="details-row">

            <span>
              Date & time
            </span>

            <strong>
              {formatDate(
                transaction.createdAt
              )}
            </strong>

          </div>

        </div>

        <div className="details-demo-notice">

          <span>
            ⓘ
          </span>

          <p>
            This transaction belongs to
            the WalletPay demo project.
            No real money was transferred.
          </p>

        </div>

        <button
          className="details-primary-button"
          onClick={() =>
            navigate(
              "/dashboard"
            )
          }
        >
          Back to dashboard
        </button>

      </div>

    </div>
  );
}

export default TransactionDetailsPage;
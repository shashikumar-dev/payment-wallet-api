import {
  useRef,
  useState,
} from "react";

import { useNavigate } from "react-router-dom";

import apiClient from "../api/apiClient";

import "./WalletActionPage.css";

function SendMoneyPage() {
  const navigate = useNavigate();

  const [walletPayId, setWalletPayId] =
    useState("");

  const [recipient, setRecipient] =
    useState(null);

  const [amount, setAmount] =
    useState("");

  const [lookupLoading, setLookupLoading] =
    useState(false);

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  const [success, setSuccess] =
    useState(null);

  /*
   * One key represents one logical payment.
   *
   * If a request fails because of a temporary
   * network problem, retrying can use the same key.
   */
  const idempotencyKeyRef =
    useRef(null);

  const normalizeWalletPayId = () => {
    let normalized = walletPayId
      .trim()
      .toLowerCase();

    if (
      normalized &&
      !normalized.endsWith(
        "@walletpay"
      )
    ) {
      normalized =
        `${normalized}@walletpay`;
    }

    return normalized;
  };

  // ---------------------------------------------------------
  // FIND RECIPIENT
  // ---------------------------------------------------------

  const findRecipient = async () => {
    setError("");
    setRecipient(null);

    const normalizedWalletPayId =
      normalizeWalletPayId();

    if (!normalizedWalletPayId) {
      setError(
        "Please enter a WalletPay ID."
      );

      return;
    }

    setLookupLoading(true);

    try {
      const response =
        await apiClient.get(
          "/users/lookup",
          {
            params: {
              walletPayId:
                normalizedWalletPayId,
            },
          }
        );

      setRecipient(response.data);

      /*
       * Replace whatever user typed with
       * canonical backend value.
       */
      setWalletPayId(
        response.data.walletPayId
      );

    } catch (err) {
      if (
        err.response?.status === 404
      ) {
        setError(
          "WalletPay user not found."
        );
      } else if (
        err.response?.status === 401
      ) {
        setError(
          "Your session has expired. Please sign in again."
        );
      } else if (
        err.response?.status === 429
      ) {
        setError(
          "Too many requests. Please wait a moment and try again."
        );
      } else {
        setError(
          err.response?.data?.message ||
          "Unable to find this WalletPay user."
        );
      }
    } finally {
      setLookupLoading(false);
    }
  };

  // ---------------------------------------------------------
  // WALLETPAY ID CHANGE
  // ---------------------------------------------------------

  const handleWalletPayIdChange = (
    event
  ) => {
    setWalletPayId(
      event.target.value
    );

    /*
     * Recipient was verified against the
     * previous WalletPay ID.
     *
     * Once the text changes, it must be
     * verified again.
     */
    setRecipient(null);

    idempotencyKeyRef.current =
      null;

    if (error) {
      setError("");
    }
  };

  // ---------------------------------------------------------
  // AMOUNT CHANGE
  // ---------------------------------------------------------

  const handleAmountChange = (
    event
  ) => {
    setAmount(
      event.target.value
    );

    /*
     * Changing payment amount means this is
     * now a different logical payment.
     */
    idempotencyKeyRef.current =
      null;

    if (error) {
      setError("");
    }
  };

  // ---------------------------------------------------------
  // SEND PAYMENT
  // ---------------------------------------------------------

  const handleSubmit = async (
    event
  ) => {
    event.preventDefault();

    setError("");

    if (!recipient) {
      setError(
        "Please find and confirm the recipient first."
      );

      return;
    }

    const numericAmount =
      Number(amount);

    if (
      !Number.isFinite(
        numericAmount
      ) ||
      numericAmount <= 0
    ) {
      setError(
        "Please enter a valid amount."
      );

      return;
    }

    /*
     * Generate once for this logical payment.
     */
    if (
      !idempotencyKeyRef.current
    ) {
      idempotencyKeyRef.current =
        crypto.randomUUID();
    }

    setLoading(true);

    try {
      const response =
        await apiClient.post(
          "/transfers",
          {
            receiverWalletPayId:
              recipient.walletPayId,

            amount:
              numericAmount,
          },
          {
            headers: {
              "Idempotency-Key":
                idempotencyKeyRef.current,
            },
          }
        );

      setSuccess(
        response.data
      );

    } catch (err) {
      const status =
        err.response?.status;

      if (status === 400) {
        setError(
          err.response?.data?.message ||
          "Please check your payment details."
        );
      } else if (
        status === 404
      ) {
        setError(
          err.response?.data?.message ||
          "Recipient wallet was not found."
        );
      } else if (
        status === 409
      ) {
        setError(
          err.response?.data?.message ||
          "The transfer could not be completed."
        );
      } else if (
        status === 401
      ) {
        setError(
          "Your session has expired. Please sign in again."
        );
      } else if (
        status === 429
      ) {
        setError(
          "Too many requests. Please wait a moment and try again."
        );
      } else {
        setError(
          err.response?.data?.message ||
          "Transfer failed. Please try again."
        );
      }
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------
  // RESET
  // ---------------------------------------------------------

  const resetPayment = () => {
    setWalletPayId("");
    setRecipient(null);
    setAmount("");
    setSuccess(null);
    setError("");

    idempotencyKeyRef.current =
      null;
  };

  // ---------------------------------------------------------
  // SUCCESS SCREEN
  // ---------------------------------------------------------

  if (success) {
    return (
      <div className="action-page">

        <div className="action-page-card success-card">

          <div className="success-icon">
            ✓
          </div>

          <p className="action-label">
            TRANSFER COMPLETE
          </p>

          <h1>
            Money sent!
          </h1>

          <p className="success-description">
            Your transfer was completed
            successfully.
          </p>

          <div className="success-amount">
            {new Intl.NumberFormat(
              "en-IN",
              {
                style: "currency",
                currency: "INR",
                minimumFractionDigits: 2,
              }
            ).format(
              Number(success.amount)
            )}
          </div>

          <div className="transfer-details">

            <div>
              <span>
                Recipient
              </span>

              <strong>
                {recipient?.name || "—"}
              </strong>
            </div>

            <div>
              <span>
                WalletPay ID
              </span>

              <strong>
                {
                  recipient?.walletPayId ||
                  "—"
                }
              </strong>
            </div>

            <div>
              <span>
                Transfer ID
              </span>

              <strong>
                #{success.transferId}
              </strong>
            </div>

            <div>
              <span>
                Status
              </span>

              <strong className="status-success">
                {success.status}
              </strong>
            </div>

          </div>

          <button
            className="wallet-primary-button"
            onClick={() =>
              navigate(
                "/dashboard"
              )
            }
          >
            Back to dashboard
          </button>

          <button
            className="secondary-button"
            onClick={resetPayment}
          >
            Send another payment
          </button>

        </div>

      </div>
    );
  }

  // ---------------------------------------------------------
  // TRANSFER FORM
  // ---------------------------------------------------------

  return (
    <div className="action-page">

      <div className="action-page-card">

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

        <div className="action-page-header">

          <div className="large-action-icon">
            ↑
          </div>

          <div>

            <p className="action-label">
              TRANSFER
            </p>

            <h1>
              Send money
            </h1>

            <p>
              Transfer money using a
              WalletPay ID.
            </p>

          </div>

        </div>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>

          <div className="wallet-form-group">

            <label htmlFor="walletPayId">
              WalletPay ID
            </label>

            <div className="recipient-search">

              <input
                id="walletPayId"
                type="text"
                placeholder="Enter WalletPay ID"
                value={walletPayId}
                onChange={
                  handleWalletPayIdChange
                }
                autoComplete="off"
                required
                disabled={
                  loading ||
                  lookupLoading
                }
              />

              <button
                type="button"
                className="find-user-button"
                onClick={
                  findRecipient
                }
                disabled={
                  loading ||
                  lookupLoading ||
                  !walletPayId.trim()
                }
              >
                {lookupLoading
                  ? "Finding..."
                  : "Find"}
              </button>

            </div>

            <small>
              Enter the WalletPay ID of
              the person you want to pay.
            </small>

          </div>

          {recipient && (
            <div className="recipient-card">

              <div className="recipient-avatar">
                {recipient.name
                  ?.charAt(0)
                  ?.toUpperCase() ||
                  "U"}
              </div>

              <div className="recipient-info">

                <strong>
                  {recipient.name}
                </strong>

                <span>
                  {
                    recipient.walletPayId
                  }
                </span>

              </div>

              <div className="recipient-check">
                ✓
              </div>

            </div>
          )}

          <div className="amount-field">

            <label htmlFor="amount">
              Amount
            </label>

            <div className="money-input">

              <span>
                ₹
              </span>

              <input
                id="amount"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="0.00"
                value={amount}
                onChange={
                  handleAmountChange
                }
                required
                disabled={loading}
              />

            </div>

          </div>

          <div className="quick-amounts">

            {[100, 500, 1000, 2000].map(
              (value) => (
                <button
                  key={value}
                  type="button"
                  disabled={loading}
                  onClick={() => {
                    setAmount(
                      String(value)
                    );

                    idempotencyKeyRef.current =
                      null;

                    if (error) {
                      setError("");
                    }
                  }}
                >
                  ₹
                  {value.toLocaleString(
                    "en-IN"
                  )}
                </button>
              )
            )}

          </div>

          <div className="info-box">

            <span>
              ⓘ
            </span>

            <p>
              This is a demo wallet.
              No real money is transferred.
            </p>

          </div>

          <button
            className="wallet-primary-button"
            type="submit"
            disabled={
              loading ||
              lookupLoading ||
              !recipient
            }
          >
            {loading
              ? "Processing transfer..."
              : recipient && amount
                ? `Send ${new Intl.NumberFormat(
                  "en-IN",
                  {
                    style: "currency",
                    currency: "INR",
                    maximumFractionDigits: 2,
                  }
                ).format(
                  Number(amount || 0)
                )}`
                : "Send money"}
          </button>

        </form>

      </div>

    </div>
  );
}

export default SendMoneyPage;
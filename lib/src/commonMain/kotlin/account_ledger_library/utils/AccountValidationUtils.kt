package account_ledger_library.utils

import kotlin.jvm.JvmStatic

/**
 * Reusable account-validation guards intended to be invoked by every
 * client (Kotlin CLI, Android, desktop) BEFORE issuing an account
 * INSERT or UPDATE request, so the request fails fast at the client
 * boundary instead of round-tripping through the network and DB.
 *
 * These guards are the app-layer half of defense-in-depth against
 * rules that the SQL foreign key alone cannot express. The DB-side
 * half lives in the trg_accounts_no_self_parent_ins / _upd triggers
 * on the accounts table (installed per the
 * mariadb-check-autoincrement-trigger-fallback skill). Both halves
 * MUST be present; either alone is insufficient.
 */
object AccountValidationUtils {

    /**
     * Returns true when the proposed [parentAccountId] is a valid
     * parent reference for an account whose primary key is
     * [accountId]. A null parent is always valid (it represents a
     * root account). A non-null parent equal to the account's own
     * id is INVALID — that would create a self-referential row
     * which the SQL CHECK constraint cannot express on AUTO_INCREMENT
     * primary keys (MariaDB error 1901) and which the DB triggers
     * reject with SQLSTATE 45000.
     *
     * Intended for guard clauses immediately before issuing the
     * update-account HTTP request:
     *
     *     if (!AccountValidationUtils.isValidParentAccountId(
     *             accountId = currentAccountId,
     *             parentAccountId = newParentAccountId,
     *         )
     *     ) {
     *         showError("Parent account must not be the account itself")
     *         return
     *     }
     */
    @JvmStatic
    fun isValidParentAccountId(
        accountId: UInt,
        parentAccountId: UInt?,
    ): Boolean = parentAccountId == null || parentAccountId != accountId

    /**
     * Throws [IllegalArgumentException] with a descriptive message
     * when the parent reference is invalid; returns Unit on success.
     * Use this overload when the caller wants the failure to
     * propagate as an exception rather than a boolean.
     */
    @JvmStatic
    fun requireValidParentAccountId(
        accountId: UInt,
        parentAccountId: UInt?,
    ) {
        require(isValidParentAccountId(accountId, parentAccountId)) {
            "parent_account_id ($parentAccountId) must not equal account_id ($accountId) " +
                "(self-reference forbidden)"
        }
    }
}

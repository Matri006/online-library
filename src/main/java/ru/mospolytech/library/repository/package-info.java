/**
 * Database access: SQL, row mapping, PostgreSQL procedures and locks. Repositories accept domain
 * entities and return entities or typed read projections. Their callers in the service layer own
 * transaction boundaries, so locks and audit context share the business operation's transaction.
 */
package ru.mospolytech.library.repository;

/**
 * Application layer: use cases, inbound and outbound ports.
 *
 * <p>Depends only on the domain and {@code spring-tx} ({@code @Transactional} annotation and
 * {@code TransactionOperations}). Use-case classes carry no Spring stereotypes;
 * beans are assembled in the presentation module.
 */
package kz.company.notification.application;

/**
 * Onion architecture adapters.
 *
 * <ul>
 *     <li>{@code in.rest} — REST API;</li>
 *     <li>{@code in.messaging} — Kafka listeners (inbound commands and channel workers);</li>
 *     <li>{@code in.events} — internal domain event listeners;</li>
 *     <li>{@code in.scheduling} — scheduler jobs;</li>
 *     <li>{@code out.persistence} — JPA aggregate store;</li>
 *     <li>{@code out.messaging} — outbox and Kafka publication;</li>
 *     <li>{@code out.channel.*} — channel senders and their providers;</li>
 *     <li>{@code out.template} — Thymeleaf template rendering from DB;</li>
 *     <li>{@code out.recipient} — recipient contact resolution.</li>
 * </ul>
 * No business rules here — only protocol translation to and from the domain model.
 */
package kz.company.notification.infrastructure;

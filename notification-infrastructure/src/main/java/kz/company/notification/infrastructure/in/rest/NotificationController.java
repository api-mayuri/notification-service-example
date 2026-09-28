package kz.company.notification.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.company.notification.application.port.in.BatchItemResult;
import kz.company.notification.application.port.in.CancelNotificationUseCase;
import kz.company.notification.application.port.in.GetNotificationQuery;
import kz.company.notification.application.port.in.NotificationSearchCriteria;
import kz.company.notification.application.port.in.NotificationSummaryView;
import kz.company.notification.application.port.in.NotificationView;
import kz.company.notification.application.port.in.PageQuery;
import kz.company.notification.application.port.in.PageResult;
import kz.company.notification.application.port.in.SearchNotificationsQuery;
import kz.company.notification.application.port.in.SendBatchCommand;
import kz.company.notification.application.port.in.SendBatchUseCase;
import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.application.port.in.SendNotificationResult;
import kz.company.notification.application.port.in.SendNotificationUseCase;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationStatus;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.infrastructure.in.common.NotificationCommandMapper;
import kz.company.notification.infrastructure.in.common.NotificationRequestPayload;
import kz.company.notification.infrastructure.in.rest.dto.BatchRequest;
import kz.company.notification.infrastructure.in.rest.dto.BatchResponse;
import kz.company.notification.infrastructure.in.rest.dto.NotificationAcceptedResponse;
import kz.company.notification.infrastructure.in.rest.dto.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** REST API уведомлений. */
@RestController
@Validated
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Отправка уведомлений и отслеживание доставки")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private final SendNotificationUseCase sendNotificationUseCase;
    private final SendBatchUseCase sendBatchUseCase;
    private final CancelNotificationUseCase cancelNotificationUseCase;
    private final GetNotificationQuery getNotificationQuery;
    private final SearchNotificationsQuery searchNotificationsQuery;
    private final NotificationCommandMapper commandMapper;

    public NotificationController(SendNotificationUseCase sendNotificationUseCase, SendBatchUseCase sendBatchUseCase,
                                  CancelNotificationUseCase cancelNotificationUseCase,
                                  GetNotificationQuery getNotificationQuery,
                                  SearchNotificationsQuery searchNotificationsQuery,
                                  NotificationCommandMapper commandMapper) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.sendBatchUseCase = sendBatchUseCase;
        this.cancelNotificationUseCase = cancelNotificationUseCase;
        this.getNotificationQuery = getNotificationQuery;
        this.searchNotificationsQuery = searchNotificationsQuery;
        this.commandMapper = commandMapper;
    }

    @PostMapping
    @Operation(summary = "Принять уведомление",
            description = "Scope: notifications:send; для поля content дополнительно notifications:send-raw. "
                    + "Повтор с тем же Idempotency-Key возвращает исходный результат (заголовок Idempotent-Replayed: true).")
    @Parameter(name = IDEMPOTENCY_KEY, in = ParameterIn.HEADER, required = true, description = "Уникальный ключ запроса")
    @ApiResponse(responseCode = "202", description = "Принято; Location — адрес статуса")
    @ApiResponse(responseCode = "400", description = "Нет Idempotency-Key или тело не проходит валидацию")
    @ApiResponse(responseCode = "401", description = "Нет или неверный токен")
    @ApiResponse(responseCode = "403", description = "Нет нужного scope")
    @ApiResponse(responseCode = "409", description = "Запрос с тем же ключом обрабатывается параллельно")
    @ApiResponse(responseCode = "422", description = "Нарушены бизнес-правила или ключ повторён с другим телом")
    @ApiResponse(responseCode = "429", description = "Превышен лимит запросов клиента; см. Retry-After")
    public ResponseEntity<NotificationAcceptedResponse> send(@RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
                                                             @Valid @RequestBody NotificationRequestPayload request,
                                                             Authentication authentication) {
        SendNotificationCommand command = commandMapper.toCommand(request, AuthenticatedClient.clientId(authentication),
                new IdempotencyKey(idempotencyKey), AuthenticatedClient.hasScope(authentication, ApiScopes.SEND_RAW));
        SendNotificationResult result = sendNotificationUseCase.send(command);
        ResponseEntity.BodyBuilder response = ResponseEntity.accepted().location(location(result.notificationId().value()));
        if (result.outcome() == SendNotificationResult.Outcome.REPLAYED) {
            response.header(IDEMPOTENT_REPLAYED, "true");
        }
        return response.body(new NotificationAcceptedResponse(result.notificationId().value(), result.status().name(),
                result.outcome().name()));
    }

    @PostMapping("/batch")
    @Operation(summary = "Принять пакет до 500 уведомлений",
            description = "Scope: notifications:send. Каждый элемент обрабатывается отдельно; ответ 207 Multi-Status "
                    + "с HTTP-кодом по каждому элементу. Ключ элемента — его idempotencyKey или '<Idempotency-Key>:<индекс>'.")
    @Parameter(name = IDEMPOTENCY_KEY, in = ParameterIn.HEADER, required = true)
    @ApiResponse(responseCode = "207", description = "Результат по каждому элементу")
    @ApiResponse(responseCode = "400", description = "Тело не проходит валидацию или больше 500 элементов")
    public ResponseEntity<BatchResponse> sendBatch(@RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
                                                   @Valid @RequestBody BatchRequest request,
                                                   Authentication authentication) {
        ClientId clientId = AuthenticatedClient.clientId(authentication);
        boolean rawAllowed = AuthenticatedClient.hasScope(authentication, ApiScopes.SEND_RAW);
        IdempotencyKey batchKey = new IdempotencyKey(idempotencyKey);

        List<BatchResponse.Item> items = new ArrayList<>();
        List<Integer> originalIndexes = new ArrayList<>();
        List<SendNotificationCommand> commands = new ArrayList<>();
        for (int index = 0; index < request.items().size(); index++) {
            NotificationRequestPayload item = request.items().get(index);
            try {
                IdempotencyKey key = item.idempotencyKey() != null ? new IdempotencyKey(item.idempotencyKey())
                        : batchKey.forBatchItem(index);
                commands.add(commandMapper.toCommand(item, clientId, key, rawAllowed));
                originalIndexes.add(index);
            } catch (DomainException invalid) {
                items.add(rejected(index, invalid.errorCode(), invalid.kind().name(), invalid.getMessage()));
            }
        }
        if (!commands.isEmpty()) {
            for (BatchItemResult result : sendBatchUseCase.sendBatch(new SendBatchCommand(null, commands))) {
                int index = originalIndexes.get(result.index());
                items.add(switch (result) {
                    case BatchItemResult.Accepted accepted -> new BatchResponse.Item(index, HttpStatus.ACCEPTED.value(),
                            accepted.result().notificationId().value(), accepted.result().status().name(),
                            accepted.result().outcome().name(), null, null);
                    case BatchItemResult.Rejected rejected -> rejected(index, rejected.errorCode(), rejected.kind(),
                            rejected.message());
                });
            }
        }
        items.sort(Comparator.comparingInt(BatchResponse.Item::index));
        long acceptedCount = items.stream().filter(item -> item.status() == HttpStatus.ACCEPTED.value()).count();
        return ResponseEntity.status(HttpStatus.MULTI_STATUS)
                .body(new BatchResponse((int) acceptedCount, items.size() - (int) acceptedCount, items));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Статус уведомления с разбивкой по доставкам", description = "Scope: notifications:read")
    @ApiResponse(responseCode = "200", description = "Уведомление")
    @ApiResponse(responseCode = "404", description = "Нет уведомления этого отправителя")
    public NotificationView get(@PathVariable UUID id, Authentication authentication) {
        return getNotificationQuery.get(AuthenticatedClient.clientId(authentication), new NotificationId(id));
    }

    @GetMapping
    @Operation(summary = "Поиск уведомлений", description = "Scope: notifications:read. Новые первыми.")
    public PageResponse<NotificationSummaryView> search(
            @RequestParam(required = false) String recipientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size,
            Authentication authentication) {
        NotificationSearchCriteria criteria = new NotificationSearchCriteria(
                AuthenticatedClient.clientId(authentication),
                recipientId == null ? null : new RecipientId(recipientId),
                RequestValues.enumValue(NotificationStatus.class, status, "status"),
                RequestValues.channel(channel),
                from, to);
        PageResult<NotificationSummaryView> result = searchNotificationsQuery.search(criteria, new PageQuery(page, size));
        return new PageResponse<>(result.items(), result.page(), result.size(), result.totalItems(), result.totalPages());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить уведомление, пока отправка не началась",
            description = "Scope: notifications:send. Повторная отмена возвращает 200.")
    @ApiResponse(responseCode = "200", description = "Отменено")
    @ApiResponse(responseCode = "404", description = "Нет уведомления этого отправителя")
    @ApiResponse(responseCode = "409", description = "Отправка уже началась или завершилась")
    public NotificationView cancel(@PathVariable UUID id, Authentication authentication) {
        return cancelNotificationUseCase.cancel(AuthenticatedClient.clientId(authentication), new NotificationId(id));
    }

    private static URI location(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/notifications/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    private static BatchResponse.Item rejected(int index, String errorCode, String kind, String detail) {
        return new BatchResponse.Item(index, ProblemStatus.forKind(kind).value(), null, null, null, errorCode, detail);
    }
}

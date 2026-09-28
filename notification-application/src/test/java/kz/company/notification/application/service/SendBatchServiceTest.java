package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.port.in.BatchItemResult;
import kz.company.notification.application.port.in.SendBatchCommand;
import kz.company.notification.application.port.in.SendNotificationResult;
import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.IdempotencyKey;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static kz.company.notification.application.ApplicationTestContext.command;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendBatchServiceTest {

    private final ApplicationTestContext context = new ApplicationTestContext();

    @Test
    void eachItemIsProcessedSeparately() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        SendBatchCommand batch = new SendBatchCommand(new IdempotencyKey("batch-1"), List.of(
                command(null, Category.TRANSACTIONAL, ChannelType.EMAIL),
                command(null, Category.TRANSACTIONAL, ChannelType.SMS),
                command("own-key", Category.MARKETING, ChannelType.EMAIL)));

        List<BatchItemResult> results = context.batchService.sendBatch(batch);

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).isInstanceOf(BatchItemResult.Accepted.class);
        BatchItemResult.Rejected rejected = (BatchItemResult.Rejected) results.get(1);
        assertThat(rejected.errorCode()).isEqualTo("TEMPLATE_NOT_AVAILABLE");
        assertThat(rejected.kind()).isEqualTo("VALIDATION");
        assertThat(results.get(2)).isInstanceOf(BatchItemResult.Accepted.class);
        assertThat(context.notifications.all().stream()
                .map(notification -> notification.request().idempotencyKey().value()).sorted().toList())
                .containsExactly("batch-1:0", "own-key");
    }

    @Test
    void repeatedBatchIsReplayed() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        SendBatchCommand batch = new SendBatchCommand(new IdempotencyKey("batch-1"),
                List.of(command(null, Category.TRANSACTIONAL, ChannelType.EMAIL)));
        context.batchService.sendBatch(batch);

        BatchItemResult.Accepted replay = (BatchItemResult.Accepted) context.batchService.sendBatch(batch).getFirst();

        assertThat(replay.result().outcome()).isEqualTo(SendNotificationResult.Outcome.REPLAYED);
    }

    @Test
    void batchSizeIsLimited() {
        assertThatThrownBy(() -> new SendBatchCommand(null,
                Collections.nCopies(501, command(null, Category.TRANSACTIONAL, ChannelType.EMAIL))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("500");
    }
}

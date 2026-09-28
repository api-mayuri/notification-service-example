package kz.company.notification.application.fake;

import kz.company.notification.application.port.out.RecipientResolver;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;

import java.util.HashMap;
import java.util.Map;

public final class FakeRecipientResolver implements RecipientResolver {

    private final Map<RecipientId, RecipientContacts> directory = new HashMap<>();
    private int calls;

    public void put(RecipientId recipientId, RecipientContacts contacts) {
        directory.put(recipientId, contacts);
    }

    public int calls() {
        return calls;
    }

    @Override
    public RecipientContacts resolve(RecipientId recipientId) {
        calls++;
        return directory.getOrDefault(recipientId, RecipientContacts.NONE);
    }
}

package kz.company.notification.infrastructure.out.persistence.mapper;

import kz.company.notification.domain.common.Masking;
import kz.company.notification.domain.delivery.ChannelAddress;

/**
 * Адрес из БД для канала, который сейчас выключен или чьи правила проверки изменились.
 * Отправить по нему нельзя (канала нет), но доставку можно показать и обработать.
 */
record StoredChannelAddress(String value) implements ChannelAddress {

    @Override
    public String masked() {
        return Masking.keepEdges(value, 3);
    }
}

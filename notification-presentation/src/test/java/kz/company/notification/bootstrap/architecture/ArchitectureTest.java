package kz.company.notification.bootstrap.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import kz.company.notification.application.port.out.ChannelSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Правила луковой архитектуры. Направление зависимостей уже закреплено модулями Maven;
 * здесь дополнительно проверяются границы внутри модулей и соглашения об именах.
 */
@AnalyzeClasses(packages = "kz.company.notification", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "kz.company.notification.domain..";
    private static final String APPLICATION = "kz.company.notification.application..";
    private static final String INFRASTRUCTURE = "kz.company.notification.infrastructure..";
    private static final String BOOTSTRAP = "kz.company.notification.bootstrap..";

    @ArchTest
    static final ArchRule domainDependsOnlyOnJdk = classes().that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN)
            .because("домен — чистая Java без фреймворков");

    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomainAndSpringTx = classes().that().resideInAPackage(APPLICATION)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN, APPLICATION,
                    "org.springframework.transaction..", "org.springframework.dao..")
            .because("из Spring слою приложения разрешён только spring-tx (docs/adr/0003)");

    @ArchTest
    static final ArchRule innerLayersHaveNoSpringStereotypes = noClasses().that()
            .resideInAnyPackage(DOMAIN, APPLICATION)
            .should().beAnnotatedWith(Component.class)
            .orShould().beAnnotatedWith(Service.class)
            .orShould().beAnnotatedWith(Repository.class)
            .because("сценарии собираются в bootstrap");

    @ArchTest
    static final ArchRule infrastructureDoesNotDependOnBootstrap = noClasses().that().resideInAPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage(BOOTSTRAP);

    @ArchTest
    static final ArchRule inboundAdaptersDoNotUseOutboundAdapters = noClasses()
            .that().resideInAPackage("kz.company.notification.infrastructure.in..")
            .should().dependOnClassesThat().resideInAPackage("kz.company.notification.infrastructure.out..")
            .because("входящие адаптеры работают только через сценарии приложения");

    @ArchTest
    static final ArchRule outboundAdaptersDoNotUseInboundAdapters = noClasses()
            .that().resideInAPackage("kz.company.notification.infrastructure.out..")
            .should().dependOnClassesThat().resideInAPackage("kz.company.notification.infrastructure.in..");

    @ArchTest
    static final ArchRule controllersAreNamedAndPlaced = classes().that().areAnnotatedWith(RestController.class)
            .and().resideInAPackage(INFRASTRUCTURE)
            .should().haveSimpleNameEndingWith("Controller")
            .andShould().resideInAPackage("..infrastructure.in.rest..");

    @ArchTest
    static final ArchRule jpaEntitiesAreNamedAndPlaced = classes().that().areAnnotatedWith(Entity.class)
            .should().haveSimpleNameEndingWith("JpaEntity")
            .andShould().resideInAPackage("..infrastructure.out.persistence.entity..");

    @ArchTest
    static final ArchRule channelSendersAreNamedAndPlaced = classes().that().implement(ChannelSender.class)
            .and().resideInAPackage(INFRASTRUCTURE)
            .and().doNotHaveSimpleName("ResilientChannelSender")
            .should().haveSimpleNameEndingWith("ChannelSender")
            .andShould().resideInAPackage("..infrastructure.out.channel..");

    @ArchTest
    static final ArchRule noFieldInjection = noFields().should().beAnnotatedWith(Autowired.class)
            .because("только конструкторное внедрение");
}

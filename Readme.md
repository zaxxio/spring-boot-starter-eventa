# Event-Sourcing and Command Query Responsibility Segregation (CQRS)

# Getting Started

**Eventa Infrastructure Ready Event-Sourcing and Command Query Responsibility Segregation (CQRS)**

## Overview

Eventa offers a robust framework for implementing Command Query Responsibility Segregation (CQRS) and Event Sourcing in
Java applications. It's designed to separate command handling (write operations) from query handling (read operations),
capturing all changes to the application state as a sequence of events. This approach ensures a reliable audit trail and
state reconstruction. 

Sample : [Event-Sourcing & CQRS](https://github.com/zaxxio/event-driven-architecture)

## Maven Dependency

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml

<dependency>
    <groupId>com.github.zaxxio</groupId>
    <artifactId>spring-boot-starter-eventa</artifactId>
    <version>0.0.2</version>
</dependency>
```

## Aggregate

```java

@Getter
@Setter
@ToString
@Aggregate
@NoArgsConstructor
public class AccountAggregate extends AggregateRoot {

    @RoutingKey
    private UUID id;
    private String name;
    private String email;
    private double balance;
    private String accountType;

    @CommandHandler
    public AccountAggregate(CreateAccountCommand createAccountCommand) {

        if (createAccountCommand.getAccountName() == null || createAccountCommand.getAccountName().isEmpty()) {
            throw new RuntimeException("Account name can not be null or empty.");
        }

        apply(
                AccountCreatedEvent.builder()
                        .accountId(createAccountCommand.getAccountId())
                        .email(createAccountCommand.getEmail())
                        .accountName(createAccountCommand.getAccountName())
                        .accountType(createAccountCommand.getAccountType())
                        .balance(createAccountCommand.getBalance())
                        .build()
        );
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent accountCreatedEvent) {
        this.id = accountCreatedEvent.getAccountId();
        this.name = accountCreatedEvent.getAccountName();
        this.email = accountCreatedEvent.getEmail();
        this.balance = accountCreatedEvent.getBalance();
        this.accountType = accountCreatedEvent.getAccountType();
    }

    @CommandHandler
    public void handle(UpdateAccountCommand updateAccountCommand) {

        if (updateAccountCommand.getAccountName() == null || updateAccountCommand.getAccountName().isEmpty()) {
            throw new RuntimeException("Account name can not be null or empty.");
        }

        apply(
                AccountUpdatedEvent.builder()
                        .accountId(updateAccountCommand.getAccountId())
                        .accountName(updateAccountCommand.getAccountName())
                        .email(updateAccountCommand.getEmail())
                        .balance(updateAccountCommand.getBalance())
                        .accountType(updateAccountCommand.getAccountType())
                        .build()
        );
    }

    @EventSourcingHandler
    public void on(AccountUpdatedEvent accountUpdatedEvent) {
        this.id = accountUpdatedEvent.getAccountId();
        this.name = accountUpdatedEvent.getAccountName();
        this.email = accountUpdatedEvent.getEmail();
        this.balance = accountUpdatedEvent.getBalance();
        this.accountType = accountUpdatedEvent.getAccountType();
    }

    @CommandHandler
    public void handle(DeleteAccountCommand deleteAccountCommand) {

        if (deleteAccountCommand.getAccountName() == null || deleteAccountCommand.getAccountName().isEmpty()) {
            throw new RuntimeException("Account name can not be null or empty.");
        }

        apply(
                AccountDeletedEvent.builder()
                        .accountId(deleteAccountCommand.getAccountId())
                        .email(deleteAccountCommand.getEmail())
                        .accountName(deleteAccountCommand.getAccountName())
                        .accountType(deleteAccountCommand.getAccountType())
                        .balance(deleteAccountCommand.getBalance())
                        .build()
        );
    }

    @EventSourcingHandler
    public void on(AccountDeletedEvent accountDeletedEvent) {
        this.id = accountDeletedEvent.getAccountId();
        this.name = accountDeletedEvent.getAccountName();
        this.email = accountDeletedEvent.getEmail();
        this.balance = accountDeletedEvent.getBalance();
        this.accountType = accountDeletedEvent.getAccountType();
    }

}
```

## Projection

```java

@Log4j2
@Service
@RequiredArgsConstructor
@ProjectionGroup(name = "account-group")
public class AccountProjection {

    private final AccountRepository accountRepository;

    @EventHandler
    public void on(AccountCreatedEvent accountCreatedEvent) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountCreatedEvent.getAccountId());
        accountEntity.setEmail(accountCreatedEvent.getEmail());
        accountEntity.setAccountType(accountCreatedEvent.getAccountType());
        accountEntity.setAccountName(accountCreatedEvent.getAccountName());
        accountEntity.setBalance(accountCreatedEvent.getBalance());
        accountRepository.save(accountEntity);
        log.info("Account Created {}", accountCreatedEvent);
        printThreadId();
    }

    @EventHandler
    public void on(AccountUpdatedEvent accountUpdatedEvent) {
        Optional<AccountEntity> optionalAccountEntity = accountRepository.findById(accountUpdatedEvent.getAccountId());
        if (optionalAccountEntity.isPresent()) {
            AccountEntity accountEntity = optionalAccountEntity.get();
            accountEntity.setEmail(accountUpdatedEvent.getEmail());
            accountEntity.setAccountType(accountUpdatedEvent.getAccountType());
            accountEntity.setAccountName(accountUpdatedEvent.getAccountName());
            accountEntity.setBalance(accountUpdatedEvent.getBalance());
            this.accountRepository.save(accountEntity);
        }
        log.info("Account Updated {}", accountUpdatedEvent);
        printThreadId();
    }

    @EventHandler
    public void on(AccountDeletedEvent accountDeletedEvent) {
        accountRepository.deleteById(accountDeletedEvent.getAccountId());
        log.info("Account Deleted {}", accountDeletedEvent);
        printThreadId();
    }

    @QueryHandler
    public AccountEntity findByAccountId(FindByAccountIdQuery findByAccountIdQuery) {
        return accountRepository.findById(findByAccountIdQuery.getAccountId()).orElseThrow();
    }

    @QueryHandler
    public List<AccountEntity> findAll(FindAllAccountsQuery findAllAccountsQuery) {
        return accountRepository.findAll();
    }

    @ResetHandler
    public void deleteRepository() {
        this.accountRepository.deleteAll();
        log.info("Deleted Account Group Repository");
    }

    private static void printThreadId() {
        log.info("Thread Id : {}", Thread.currentThread().getId());
    }

}
```

## Command Dispatcher

```java

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountCommandController {

    private final CommandDispatcher commandDispatcher;
    private final EventProcessingHandler eventProcessingHandler;

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@RequestBody @Valid CreateAccountCommand createAccountCommand) throws Exception {
        this.commandDispatcher.dispatch(createAccountCommand, ((commandMessage, commandResultMessage) -> {
            if (commandResultMessage.isExceptional()) {
                log.error(commandResultMessage.getException().getMessage());
            } else {
                log.info(commandResultMessage.getResult());
            }
        }));
        return ResponseEntity.ok(createAccountCommand);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateAccount(@RequestBody @Valid UpdateAccountCommand updateAccountCommand) throws Exception {
        this.commandDispatcher.dispatch(updateAccountCommand, ((commandMessage, commandResultMessage) -> {
            if (commandResultMessage.isExceptional()) {
                log.error(commandResultMessage.getException().getMessage());
            } else {
                log.info(commandResultMessage.getResult());
            }
        }));
        return ResponseEntity.ok(updateAccountCommand);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(@RequestBody @Valid DeleteAccountCommand deleteAccountCommand) throws Exception {
        this.commandDispatcher.dispatch(deleteAccountCommand, ((commandMessage, commandResultMessage) -> {
            if (commandResultMessage.isExceptional()) {
                log.error(commandResultMessage.getException().getMessage());
            } else {
                log.info(commandResultMessage.getResult());
            }
        }));
        return ResponseEntity.ok(deleteAccountCommand);
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restoreDB() throws Exception {
        this.eventProcessingHandler.eventProcessor("account-group").reset();
        return ResponseEntity.ok("Restored Database.");
    }

}
```

## Query Dispatcher

```java

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountQueryController {

    private final QueryDispatcher queryDispatcher;

    @GetMapping("/findByAccountId")
    public ResponseEntity<?> findByAccountId(@RequestParam @Valid UUID accountId) throws Exception {
        final FindByAccountIdQuery findByProductIdQuery = FindByAccountIdQuery.builder()
                .accountId(accountId)
                .build();
        final AccountEntity result = this.queryDispatcher.dispatch(findByProductIdQuery, ResponseType.instanceOf(AccountEntity.class));
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/findAllAccounts")
    public ResponseEntity<?> findAll() throws Exception {
        FindAllAccountsQuery findAllAccountsQuery = FindAllAccountsQuery.builder().build();
        final List<AccountEntity> result = this.queryDispatcher.dispatch(findAllAccountsQuery, ResponseType.multipleInstancesOf(AccountEntity.class));
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

}
```

## Command Interceptor

```java

@Log4j2
@Service
@Interceptor
public class AccountCommandInterceptor implements CommandInterceptor {
    @Override
    public void commandIntercept(BaseCommand baseCommand) throws Exception {
        if (baseCommand instanceof UpdateAccountCommand) {
            log.info("Pre CreateAccountCommand Intercepted.");
        }
    }
}
```
## Infrastructure dependency
```yaml
eventa:
  event-bus: BaseEvent
spring:
  main:
    banner-mode: off
  profiles:
    active: dev
  application:
    name: spring-boot-app
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.UUIDSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      #transaction-id-prefix: tx-
      properties:
        enable-idempotence: true
        acks: all
    consumer:
      group-id: eventa-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.UUIDDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring:
          json:
            trusted:
              packages: '*'
      auto-offset-reset: earliest
      enable-auto-commit: false
    listener:
      missing-topics-fatal: false
      ack-mode: manual
  jpa:
    open-in-view: false
  data:
    mongodb:
      repositories:
        type: imperative
      authentication-database: admin
      auto-index-creation: true
      database: eventstore
      username: username
      password: password
      port: 27017
      host: localhost
```
## Copyright
```text
Copyright 2024 Partha Sutradhar

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
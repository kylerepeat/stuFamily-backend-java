# stuFamily-backend UML Draft

This folder contains the initial UML design for `stuFamily-backend`.

## Diagram list

1. `use-case.puml`
   - Core business use cases for mini program users and admin operators.
2. `component-architecture.puml`
   - Spring Boot layered architecture and external integrations.
3. `domain-class.puml`
   - Domain model (products, orders, payments, family groups, content).
4. `sequence-purchase-payment.puml`
   - Family card/value-added service purchase and WeChat payment callback flow.
5. `sequence-family-onboarding.puml`
   - Family member enrollment after family card purchase.

## Suggested baseline stack (for next implementation step)

- Java 17
- Spring Boot 3.3.x
- Spring Security 6.x + JWT (dual filter chains: `/api/admin/**` and `/api/mini/**`)
- MyBatis-Plus 3.5.x
- PostgreSQL 16
- Maven 3.9+


# transaction-retry

Small utility library to retry transactions on serialization failures (or deadlocks, ...).

## Structure
The library is split into two subprojects:
* core: basic transaction retry handling logic.
* spring-hibernate: additional bindings for integration with spring transactional, hibernate and aspectj.


## Disclaimer
You probably don't want to use this library as-is, but rather copy it and modify it for your own particular use case.
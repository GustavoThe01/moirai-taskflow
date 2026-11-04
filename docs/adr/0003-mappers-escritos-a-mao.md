# ADR 0003 — Mappers escritos à mão em vez de MapStruct

- **Status:** Aceita
- **Data:** 2026-11-04

## Contexto
No planejamento inicial, eu ia usar o MapStruct para converter entre o domínio e as entidades JPA.
Ao implementar a persistência, vi que o MapStruct combina pouco com o meu domínio: `Project` e `Task`
têm construtores privados, nascem por métodos de fábrica (`restore`) e usam acessores sem o prefixo `get`
(`task.status()`). Além disso, os ids tipados, os opcionais e as datas em texto precisariam de conversões
personalizadas para quase todos os campos.

## Decisão
Escrever os mappers à mão, em uma única classe (`PersistenceMapper`) dentro do adapter de persistência.

## Alternativas consideradas
- **MapStruct** — gera o código e avisa no build se faltar um campo, mas exigiria configuração e conversores
  para quase tudo. Faz mais sentido nos DTOs da API, que são simples; posso reavaliar no Dia 04.
- **Expor getters e setters no domínio** — facilitaria qualquer ferramenta, mas quebraria o encapsulamento
  que protege as regras de negócio.

## Consequências
- ✅ Conversão explícita, sem geração de código nem configuração de annotation processor.
- ✅ O domínio continua sem nenhuma concessão à ferramenta de mapeamento.
- ⚠️ Um campo novo precisa ser lembrado no mapper. Os testes de integração, que comparam todos os campos
  depois de salvar e reler, são a proteção contra esse esquecimento.

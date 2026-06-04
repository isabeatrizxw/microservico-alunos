# Microserviço de Alunos

Microserviço REST em **Java 17 + Spring Boot 3** que calcula a média de um aluno e informa a data/hora do servidor. Inclui uma interface web (HTML, CSS e JavaScript puro) com tema claro/escuro e relógio em tempo real.


## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/media` | Recebe nome e duas notas; retorna a média e a mensagem de aprovação/reprovação |
| `GET` | `/api/datetime` | Retorna a data e hora atual do servidor |
| `GET` | `/` | Interface web |

### Exemplo

```bash
curl -X POST http://localhost:8080/api/media \
  -H "Content-Type: application/json" \
  -d '{"nome": "Maria", "nota1": 7, "nota2": 8}'
```

```json
{ "nome": "Maria", "media": 7.5, "mensagem": "Parabéns Maria, você foi aprovado." }
```

## Regras de negócio

A nota de corte padrão é **5** e pode ser alterada em `application.properties` (`aluno.nota-de-corte`) sem recompilar.

| Média | Resultado |
|---|---|
| acima da nota de corte | `Parabéns {nome}, você foi aprovado.` |
| igual à nota de corte | `{nome}, você não atingiu a nota de corte e foi reprovado.` |
| abaixo da nota de corte | `{nome}, você foi reprovado.` |

As notas enviadas são validadas (obrigatórias e entre 0 e 10); valores inválidos retornam `HTTP 400` com a mensagem do erro.

## Como executar

Pré-requisitos: **JDK 17** e **Maven 3.9+**.

```bash
git clone https://github.com/isabeatrizxw/microservico-alunos.git
cd microservico-alunos
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## Estrutura

```
src/main/java/com/estudante/microservico/
├── MicroservicoApplication.java   # Inicialização
├── controller/
│   ├── AlunoController.java        # POST /api/media
│   └── HorarioController.java      # GET  /api/datetime
├── service/
│   ├── AlunoService.java           # Cálculo da média
│   └── HorarioService.java         # Data/hora do servidor
├── dto/
│   ├── AlunoRequestDTO.java        # Entrada (nome, notas) + validações
│   └── AlunoResponseDTO.java       # Saída (média, mensagem)
└── exception/
    └── ValidacaoExceptionHandler.java  # Trata erros de validação (HTTP 400)
```

A organização segue camadas: **controller** (HTTP) → **service** (regras) → **dto** (transporte de dados).

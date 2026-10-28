# Microserviço de Alunos

Sistema de notas escolares em **Java 17 + Spring Boot 3**, feito para atender várias escolas ao mesmo tempo. Cada escola tem suas turmas, disciplinas, professores e alunos. Professores lançam notas, alunos acompanham o próprio boletim e o sistema calcula a média de cada aluno e a média da turma. Professores que dão aula por conta própria também podem criar uma conta e usar o sistema sozinhos.

## Funcionalidades

- **Várias escolas isoladas:** uma escola nunca vê os dados de outra.
- **Papéis definidos pela escola:** ninguém escolhe o próprio papel; professores e alunos entram por convite.
- **Turmas, disciplinas e aulas:** a escola organiza as turmas, cadastra as disciplinas e atribui a cada professor as aulas que ele dá.
- **Notas e médias:** notas por disciplina, sem limite de avaliações, com boletim do aluno e média da turma.
- **Professor autônomo:** cadastro aberto, com confirmação por e-mail, para quem dá aulas particulares.
- **Segurança:** login por sessão, proteção CSRF, senhas com BCrypt, recuperação de senha por e-mail e bloqueio após muitas tentativas.
- **Interface web** em HTML, CSS e JavaScript puro, com tema claro e escuro e cuidados de acessibilidade.

## Como executar

Pré-requisitos: **JDK 17** e **Maven 3.9+**.

```bash
git clone https://github.com/isabeatrizxw/microservico-alunos.git
cd microservico-alunos
SUPER_ADMIN_EMAIL=voce@exemplo.com SUPER_ADMIN_SENHA=uma-senha-forte mvn spring-boot:run
```

Abra `http://localhost:8080` e entre com o e-mail e a senha que você definiu. As variáveis `SUPER_ADMIN_*` só são necessárias na primeira execução: elas criam a conta do dono do sistema.

Em desenvolvimento os dados ficam num banco H2 na pasta `data/` (não precisa instalar nada) e os e-mails não são enviados: eles aparecem no log da aplicação, com os links de convite e de confirmação.

### Testes

```bash
mvn test
```

## Papéis

| Papel | Quem é | O que faz |
|---|---|---|
| `SUPER_ADMIN` | Dono do sistema | Cadastra escolas e o administrador de cada uma |
| `ADMIN_ESCOLA` | Secretaria ou coordenação | Organiza turmas e disciplinas, convida professores e alunos, atribui aulas e gerencia as pessoas da escola |
| `PROFESSOR` | Professor da escola | Lança notas nas turmas e disciplinas atribuídas a ele, vê boletins e a média da turma |
| `PROFESSOR_AUTONOMO` | Quem dá aula por conta própria | Cria a própria conta e faz tudo no seu espaço: turmas, disciplinas, convites de alunos e notas |
| `ALUNO` | Aluno | Vê o próprio boletim, com a média e a situação de cada disciplina |

## Como funciona

### Escolas

1. O dono do sistema cadastra a escola junto com a conta do administrador dela.
2. A administração cadastra as turmas e as disciplinas.
3. A administração convida professores e alunos por e-mail. Para aluno, o convite já define a turma.
4. Na aba Professores, a administração atribui as aulas (ex.: Prof. Carlos dá Matemática no 1º A).
5. O professor entra e passa a ver só as turmas e disciplinas dele.

A administração também troca alunos de turma, desativa e reativa pessoas (nada é apagado: as notas ficam no histórico) e renomeia ou exclui turmas e disciplinas que não estão em uso.

### Convites

- Quem recebe o convite abre o link, confere a escola, o papel e a turma, escolhe nome e senha e depois entra pelo login.
- O convite vale 7 dias, serve uma vez só e pode ser revogado antes de ser usado.
- O sistema guarda apenas o hash do código, por isso o link é mostrado uma única vez para quem convidou.

### Professor autônomo

1. Em `/cadastro.html` (link "Crie sua conta de professor" na tela de login), a pessoa informa nome, e-mail e senha.
2. O sistema envia um link de confirmação por e-mail, válido por 24 horas. A conta só funciona depois de confirmar.
3. O professor autônomo cadastra turmas e disciplinas, convida seus alunos e lança notas.

A resposta do cadastro é sempre a mesma, para não revelar quem já tem conta.

### Esqueci minha senha

1. Na tela de login, o link "Esqueci minha senha" leva a `/esqueci-senha.html`, onde a pessoa informa o e-mail.
2. Se houver uma conta ativa com esse e-mail, o sistema envia um link para `/redefinir-senha.html`, válido por 1 hora e de uso único.
3. A pessoa escolhe a nova senha e entra pelo login.

A resposta do pedido também é sempre a mesma, exista ou não a conta. Como nos convites, o sistema guarda só o hash do código.

### Interface

- Tema claro ou escuro: segue o sistema operacional até a pessoa escolher um no botão do topo. A escolha vale também para as páginas de login, convite, cadastro e senha.
- Cores com contraste adequado nos dois temas, botões com pelo menos 44 px de altura em telas de toque.
- Erros de validação marcam o campo com problema (`aria-invalid`), ligam o campo à mensagem e são anunciados por leitores de tela.

### Notas e médias

- Cada nota tem disciplina, descrição (ex.: "Prova 1") e valor de 0 a 10, com até 2 casas decimais.
- Só o professor que lançou uma nota pode corrigi-la ou apagá-la. O sistema guarda quem lançou e quando.
- **Média do aluno** numa disciplina: média de todas as notas dele, somadas com um laço `for` e arredondadas para 2 casas.
- **Média da turma** numa disciplina: média das médias dos alunos, também calculada com `for`. Alunos ainda sem notas aparecem como `SEM_NOTAS` e não entram na conta.

A nota de corte padrão é **5** e pode ser alterada em `application.properties` (`aluno.nota-de-corte`) sem recompilar.

| Média | Situação |
|---|---|
| acima da nota de corte | `APROVADO` |
| igual ou abaixo da nota de corte | `REPROVADO` |
| sem notas | `SEM_NOTAS` |

## API

O login usa sessão (cookie). Requisições que alteram dados precisam do token CSRF: o servidor envia o cookie `XSRF-TOKEN` e o cliente devolve o valor no header `X-XSRF-TOKEN`. Erros voltam como `{"erro": "mensagem"}` (ou `{"campo": "mensagem"}` em erros de validação).

**Acesso e cadastro**

| Método | Rota | Quem pode |
|---|---|---|
| `POST` | `/api/auth/login` | Todos |
| `GET` | `/api/auth/eu` | Logado |
| `POST` | `/api/auth/logout` | Logado |
| `POST` | `/api/cadastro/autonomo` | Todos (cria conta de professor autônomo) |
| `POST` | `/api/cadastro/confirmar` | Quem recebeu o link de confirmação |
| `POST` | `/api/senha/esqueci` | Todos (envia o link para criar nova senha) |
| `POST` | `/api/senha/redefinir` | Quem recebeu o link de nova senha |
| `GET` | `/api/convites/{codigo}` | Quem recebeu o convite |
| `POST` | `/api/convites/{codigo}/aceitar` | Quem recebeu o convite |

**Dono do sistema**

| Método | Rota | Descrição |
|---|---|---|
| `GET` / `POST` | `/api/admin/instituicoes` | Lista e cadastra escolas |

**Escola** (`ADMIN_ESCOLA`; o `PROFESSOR_AUTONOMO` também usa, no próprio espaço)

| Método | Rota | Descrição |
|---|---|---|
| `GET` / `POST` | `/api/escola/turmas` | Lista e cadastra turmas (professor também lista) |
| `PUT` / `DELETE` | `/api/escola/turmas/{id}` | Renomeia e exclui turma |
| `GET` / `POST` | `/api/escola/disciplinas` | Lista e cadastra disciplinas (professor também lista) |
| `PUT` / `DELETE` | `/api/escola/disciplinas/{id}` | Renomeia e exclui disciplina |
| `GET` / `POST` | `/api/escola/convites` | Lista convites pendentes e cria convite |
| `DELETE` | `/api/escola/convites/{id}` | Revoga convite |
| `GET` | `/api/escola/pessoas` | Lista alunos e professores |
| `PUT` | `/api/escola/alunos/{id}/turma` | Troca o aluno de turma |
| `PUT` | `/api/escola/pessoas/{id}/ativo` | Desativa ou reativa aluno ou professor |
| `POST` | `/api/escola/atribuicoes` | Atribui aula a um professor (só `ADMIN_ESCOLA`) |
| `DELETE` | `/api/escola/atribuicoes/{id}` | Remove aula atribuída (só `ADMIN_ESCOLA`) |

**Professor** (`PROFESSOR` e `PROFESSOR_AUTONOMO`)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/professor/atribuicoes` | Aulas atribuídas ao professor logado |
| `GET` | `/api/professor/turmas/{id}/alunos` | Alunos da turma |
| `GET` | `/api/professor/turmas/{id}/medias?disciplinaId=` | Média da turma na disciplina |
| `GET` | `/api/professor/alunos/{id}/boletim` | Boletim de um aluno |
| `POST` | `/api/professor/notas` | Lança nota |
| `PUT` / `DELETE` | `/api/professor/notas/{id}` | Corrige ou apaga nota (só quem lançou) |

**Aluno**

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/aluno/boletim` | Boletim do aluno logado |

## Configuração

| Variável | Para quê | Padrão |
|---|---|---|
| `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_SENHA` | Criar o primeiro dono do sistema | vazio |
| `SPRING_PROFILES_ACTIVE` | `prod` para produção | `dev` |
| `DB_URL`, `DB_USUARIO`, `DB_SENHA` | Conexão com o PostgreSQL (perfil `prod`) | obrigatórias em `prod` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Servidor de e-mail (SMTP) | obrigatórias em `prod` |
| `EMAIL_REMETENTE` | Remetente dos e-mails | `nao-responda@localhost` |
| `URL_PUBLICA` | Endereço usado nos links dos e-mails | `http://localhost:8080` |

Exemplo de execução em produção:

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://localhost:5432/microservico DB_USUARIO=usuario DB_SENHA=senha \
SPRING_MAIL_HOST=smtp.exemplo.com SPRING_MAIL_PORT=587 SPRING_MAIL_USERNAME=usuario SPRING_MAIL_PASSWORD=senha \
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true \
EMAIL_REMETENTE=nao-responda@seudominio.com URL_PUBLICA=https://seudominio.com \
mvn spring-boot:run
```

No perfil `prod` o cookie de sessão só trafega por HTTPS e a aplicação não sobe sem SMTP configurado, para nenhum link de convite ou confirmação ir parar no log.

Os limites de tentativas ficam em `application.properties` (`app.limite.*`): login por e-mail e por IP a cada 15 minutos, cadastro por IP e pedidos de nova senha por IP e por e-mail a cada hora. Acima do limite a API responde `429`.

As tabelas são criadas pelas migrações do Flyway em `src/main/resources/db/migration`.

## Estrutura

```
src/main/java/com/estudante/microservico/
├── config/       # Segurança, e-mail e criação do primeiro dono do sistema
├── controller/   # Endpoints HTTP
├── service/      # Regras de negócio (convites, cadastro, pessoas, notas e médias)
├── repository/   # Acesso ao banco (Spring Data JPA)
├── model/        # Entidades: Instituicao, Turma, Disciplina, Usuario, Convite, Nota, Atribuicao
├── dto/          # Dados de entrada e saída da API
├── security/     # Usuário logado, códigos de convite e limite de tentativas
├── email/        # Envio de e-mail
└── exception/    # Erros convertidos em respostas HTTP

src/main/resources/
├── db/migration/ # Migrações do banco (Flyway)
└── static/       # Interface web (HTML, CSS e JavaScript)
```

A organização segue camadas: **controller** (HTTP) → **service** (regras) → **repository** (banco).

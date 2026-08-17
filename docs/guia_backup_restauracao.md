# Guia de Backup e Restauração

Este guia descreve o backup lógico e a restauração do PostgreSQL usado pelo SRM Credit Engine. Os procedimentos preservam o formato custom do PostgreSQL, não dependem de arquivos de aplicação e podem ser executados contra uma instância local, externa ou pelo contêiner do `docker-compose.yml`.

## 1. Premissas

- O banco padrão do compose é `srm_credit`.
- O compose expõe o PostgreSQL em `localhost:5656`.
- As migrações atuais criam as tabelas no schema `public`; portanto, `PGSCHEMA=public` é o padrão dos scripts.
- Uma instalação que use o schema `srm_asset` deve informar `PGSCHEMA=srm_asset` nos comandos de backup e restauração.
- A senha deve ser fornecida por `PGPASSWORD`, `.pgpass` ou outro mecanismo seguro; não grave credenciais nos scripts nem no crontab.

## 2. Variáveis

| Variável | Padrão | Finalidade |
| --- | --- | --- |
| `BACKUP_DIR` | `./backups` | Diretório dos dumps |
| `BACKUP_PREFIX` | `srm_credit` | Prefixo do nome do dump |
| `RETENTION_DAYS` | `7` | Retenção mínima em dias |
| `PGHOST` | `localhost` | Host PostgreSQL quando executado no host |
| `PGPORT` | `5656` | Porta PostgreSQL quando executado no host |
| `PGDATABASE` | `srm_credit` | Banco de dados |
| `PGUSER` | `postgres` | Usuário PostgreSQL |
| `PGSCHEMA` | `public` | Schema incluído no dump |
| `PGPASSWORD` | sem padrão | Senha da conexão, se necessária |
| `USE_DOCKER` | `0` | Usa `docker compose exec` para executar o cliente no serviço `postgres` |

## 3. Backup local

Suba o PostgreSQL:

```bash
docker compose up -d postgres
```

Execute o backup usando os clientes PostgreSQL instalados no host:

```bash
PGPASSWORD=postgres ./scripts/backup.sh
```

O script gera um arquivo como `backups/srm_credit_20260817T120000Z.dump`, com permissão `0600`, em formato custom `-Fc`. O arquivo temporário só é renomeado após o `pg_dump` terminar com sucesso. Dumps com mais de `RETENTION_DAYS` dias são removidos.

Se os clientes não estiverem instalados no host, execute o cliente existente no contêiner:

```bash
PGPASSWORD=postgres USE_DOCKER=1 ./scripts/backup.sh
```

Para uma instância externa, informe as variáveis sem colocar a senha no comando quando o histórico do shell não for apropriado:

```bash
export PGHOST=db.example.internal
export PGPORT=5432
export PGDATABASE=srm_credit
export PGUSER=srm_backup
export PGSCHEMA=public
./scripts/backup.sh
```

## 4. Restauração

A restauração usa `--clean --if-exists`, substitui os objetos do schema informado e é uma operação destrutiva. Use uma base de destino isolada para o primeiro teste e confirme explicitamente a operação:

```bash
PGPASSWORD=postgres CONFIRM_RESTORE=YES ./scripts/restore.sh backups/srm_credit_20260817T120000Z.dump
```

Com os clientes dentro do contêiner:

```bash
PGPASSWORD=postgres USE_DOCKER=1 CONFIRM_RESTORE=YES ./scripts/restore.sh backups/srm_credit_20260817T120000Z.dump
```

Após o `pg_restore`, o script consulta as tabelas-chave e imprime as contagens de `taxa_cambio`, `recebivel`, `liquidacao`, `audit_log` e `usuario`. A restauração falha se qualquer tabela não existir ou se houver erro de conexão, leitura ou SQL.

O script não executa `DROP DATABASE`, não altera roles e não restaura owners ou ACLs. Esses recursos devem ser provisionados separadamente no ambiente de destino.

## 5. Agendamento

Crie o diretório de backup fora da área pública da aplicação e configure credenciais por `.pgpass` com permissão `0600`. Exemplo de entrada diária às 02:00, usando clientes no host:

```text
0 2 * * * cd /opt/srm-credit-engine && BACKUP_DIR=/var/backups/srm-credit RETENTION_DAYS=7 /opt/srm-credit-engine/scripts/backup.sh >> /var/log/srm-credit-backup.log 2>&1
```

O arquivo `.pgpass` deve conter uma linha compatível com `PGHOST`, `PGPORT`, `PGDATABASE` e `PGUSER`, e não deve ser versionado. Para backup remoto, copie o dump após validar o código de saída para um armazenamento de objetos com retenção e criptografia gerenciadas pelo ambiente.

## 6. RPO, RTO e teste periódico

- RPO sugerido: 24 horas com o cron diário; reduza o intervalo se a perda máxima aceitável for menor.
- RTO sugerido: 2 horas para restaurar o dump, reaplicar migrações pendentes e validar a aplicação.
- Teste mensal: gerar um dump, restaurá-lo em uma base isolada, executar as consultas de consistência e iniciar o backend com `ddl-auto=validate`.
- Registre data, duração, tamanho do dump, versão das migrações e resultado da restauração.

O backup não substitui alta disponibilidade, replicação ou retenção remota. O dump deve ser armazenado fora do host do banco para proteger contra perda do volume local.

## 7. Verificação rápida

```bash
bash -n scripts/backup.sh
bash -n scripts/restore.sh
PGPASSWORD=postgres USE_DOCKER=1 ./scripts/backup.sh
PGPASSWORD=postgres USE_DOCKER=1 CONFIRM_RESTORE=YES ./scripts/restore.sh backups/<arquivo>.dump
```

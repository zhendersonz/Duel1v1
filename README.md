# ⚔️ Duel1v1

**Plugin criado por [ZHendersonZ](https://github.com/zhendersonz)**

Plugin de duelos 1v1 para servidores **Paper 26.1.2+** (Minecraft 1.21.4+).  
Sistema completo com desafios, fila de duelos, arena configurável, NPC clicável, ranking holográfico e estatísticas.

---

## Requisitos

- **Servidor:** Paper 26.1.2 ou superior
- **Java:** JDK 26 (para compilar e rodar)
- **Maven:** 3.8+ (para compilar)

---

## Instalação

1. Baixe o `Duel1v1-1.0.1.jar` da [página de releases](https://github.com/zhendersonz/Duel1v1/releases)
2. Coloque o `.jar` na pasta `plugins/` do seu servidor
3. Reinicie o servidor
4. Configure a arena (veja abaixo)

### Compilar do código fonte

```bash
git clone https://github.com/zhendersonz/Duel1v1.git
cd Duel1v1
mvn clean package
```

O `.jar` compilado estará em `target/Duel1v1-1.0.1.jar`.

---

## Configuração

### Arena

Use os comandos abaixo para definir as posições da arena:

```
/duelo setpos pos1   # Define a posição do jogador 1
/duelo setpos pos2   # Define a posição do jogador 2
/duelo setpos lobby  # Define o lobby (onde voltam após o duelo)
```

### Kit

Edite o arquivo `plugins/Duel1v1/config.yml`:

```yaml
kit:
  items:
    - DIAMOND_SWORD
    - BOW:1
    - ARROW:32
  armor:
    - DIAMOND_HELMET
    - DIAMOND_CHESTPLATE
    - DIAMOND_LEGGINGS
    - DIAMOND_BOOTS
```

### Holograma de Ranking

Crie um holograma exibindo o Top 5 jogadores por K/D:

```
/duelo holograma criar   # Cria o holograma na sua localização
/duelo holograma remover # Remove o holograma
/duelo holograma atualizar # Atualiza manualmente
```

O holograma atualiza automaticamente a cada 30 segundos e mostra:

```
═══ ⚔ RANKING DUELOS ⚔ ═══
                 BATALHAS   KILLS   MORTES    K/D
#1  Jogador1        10       15        5       3.00
#2  Jogador2         8       12        6       2.00
...
```

### NPC de Fila (opcional)

1. Vá até o local desejado no lobby
2. Use o comando `/duelonpc`
3. Um **Villager** com nome "§e§lCLIQUE PARA DUELAR" aparecerá
4. Jogadores podem clicar nele (esquerdo ou direito) para entrar na fila
5. O NPC persiste entre restart do servidor

---

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/duelo <jogador>` | Desafiar um jogador para duelo | `duel.use` |
| `/duelo aceitar <jogador>` | Aceitar um desafio | `duel.use` |
| `/duelo recusar <jogador>` | Recusar um desafio | `duel.use` |
| `/duelo sair` | Sair da partida atual | `duel.use` |
| `/duelo cancelar` | Sair da fila de duelos | `duel.use` |
| `/duelo setpos <pos1\|pos2\|lobby>` | Definir posições da arena | `duel.admin` |
| `/duelo stats [jogador]` | Ver estatísticas de um jogador | `duel.use` |
| `/duelo stats resetar <jogador>` | Resetar estatísticas de um jogador | `duel.admin` |
| `/duelo holograma criar\|remover\|atualizar` | Gerenciar holograma de ranking | `duel.admin` |
| `/duelonpc` | Criar NPC clicável para fila | `duel.admin` |

---

## Permissões

| Permissão | Descrição | Padrão |
|---|---|---|
| `duel.use` | Permite usar comandos de duelo | `true` (todos) |
| `duel.admin` | Permite gerenciar arena, NPC, stats e holograma | `op` |

---

## Funcionalidades

- **Desafios 1v1**: Desafie qualquer jogador online
- **Fila de duelos**: Entre na fila e aguarde um oponente
- **Kit automático**: Receba itens e armaduras ao iniciar o duelo
- **Contagem regressiva**: 5 segundos antes do duelo começar
- **Combate em rodadas**: Ao morrer, você revive e continua até 2 mortes
- **Fireworks**: Fogos de artifício ao vencer
- **NPC clicável**: Entre na fila clicando em um NPC no lobby (esquerdo ou direito)
- **NPC imune a dano**: O NPC não pode ser danificado por nenhuma fonte
- **Combat Log**: Se desconectar durante o duelo, perde a partida mas recupera seus itens ao voltar
- **Estatísticas**: K/D, BATALHAS, KILLS, MORTES salvos em `stats.yml`
- **Ranking holográfico**: Top 5 jogadores por K/D em holograma com ArmorStands (sem plugins externos)
- **Auto-update**: Holograma atualiza automaticamente a cada 30 segundos
- **Configurável**: Arena, kit e timeout configuráveis no `config.yml`

---

## Suporte

Criado por **ZHendersonZ**.  
Issues e sugestões: [GitHub Issues](https://github.com/zhendersonz/Duel1v1/issues)

# Telegram Bot - DLRG Lette e. V.
Es handelt sich hierbei um einen Telegram-Bot der DLRG Lette 
für den schnellen Informationsaustausch. Ein "Newsletter-Bot" wenn man so will.

## Funktionen Benutzer:
- Abonnieren von verschiedenen Kategorien.
- Ändern des Abo-Status der Kategorien.
- Vergessen werden (sämtliche gespeicherten Daten werden entfernt).

## Funktionen Administratoren / Moderatoren:
- Senden einer Nachricht an alle Abonnenten einer bestimmten Kategorie.
- Hinzufügen von Moderatoren.
- Ändern von Berechtigungsrollen von Moderatoren (Admin / Moderator)
- Entfernen von Moderatoren (Nur Admins)
- Hinzufügen, Ändern und Löschen von Kategorien


# Erforderliche Komponenten
- MongoDB
- 2 Telegram Bots (bzw. Token, können über [Telegram Botfather](https://telegram.me/botfather) erstellt werden)
    - AdminBot -> Für Moderatoren und Admins
    - SenderBot -> Für die Benutzer / Abonennten
    
# Einrichtung
Nachrichten werden durch die API gepollt, es wird also KEIN freigegebener Port o.ä. benötigt. Neu in Version 2.0.0 - [#6](https://github.com/dlrg-lette/dlrg-lette-telegram-bot/issues/6)

### Erforderlich

`app.name=` Name der Spring Boot Applikation, hat keine Auswirkungen auf
das Telegram Erscheinungsbild

`auth.admin-bot-token=` Token des Admin-Bots

`auth.sender-bot-token=` Token des Sender-Bot

## MongoDB
- Konfiguration der MonoDB über `spring.data.mongodb.*` Parameter.
- Eine MonoDB Datenbank mit folgenden Collections:
    - `adminChat`
    - `category`
    - `senderChat`
    - `user`
    - `texts`

Bis auf `texts` sind alle Collections initial leer.

### Initaldaten `texts` collection

[Initialdaten der `texts` collection im JSON Format](texts.collection.initial.json)

## Docker Konfiguration

Nachfolgend finden sich Informationen zum Betrieb als Docker
Container [Docker Hub](https://hub.docker.com/r/ayokas/dlrg-lette-telegram-bot).

### Umgebungsvariablen

Im Docker Betrieb können alle wichtigen Einstellungen über Umgebungsvariablen hinterlegt werden. Alternativ können auch
bereits existierende Konfigurationen nach dem ersten Start übernommen werden, sofern sie im Volume vorhanden sind und
korrekt benannt sind.

| Variable               | Beschreibung                                                                                       | Standard                    |
|------------------------|----------------------------------------------------------------------------------------------------|:----------------------------|
| SPRING_PROFILE_ACTIVE  | Aktives Spring Boot Profil / Name der .properties Datei                                            | `application`               |
| SPRING_CONFIG_LOCATION | Beschreibt den Ordner in welchem nach der Konfiguration gesucht wird                               | `/telegram-bot-config`      |
| ADMIN_BOT_TOKEN        | Token des Admin-Bot                                                                                |                             |
| SENDER_BOT_TOKEN       | Token des Sender-Bot                                                                               |                             |
| BOT_NAME               | Siehe `app.name`                                                                                   | `mongodb`                   |
| MONGO_HOST             | MongoDB Host / IP unter dem die MongoDB erreichbar ist                                             | `27017`                     |
| MONGO_PORT             | MongoDB Port                                                                                       |                             |
| MONGO_DB               | MongoDB Datenbankname                                                                              |                             |
| MONGO_USER             | MongoDB Benutzer für Kommunikation                                                                 |                             |
| MONGO_PW               | MongoDB Kennwort                                                                                   |                             |
| MONGO_AUTH_DB          | Authentifizierungstabelle (normalerweise immer Stanard)                                            | `admin`                     |

### Volumes

Es wird ein Volume benötigt für die Konfiguration, der Pfad wird aus `SPRING_CONFIG_LOCATION` übernommen.
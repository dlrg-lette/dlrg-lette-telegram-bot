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
## SpringBoot Parameter
 Für grundlegende SpringBoot Parameter verweise ich auf die aktuelle
 Dokumentation: [SpringBoot Documentation: Appendix A: Common application
           properties](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html)

### Erforderlich

`app.name=` Name der Spring Boot Applikation, hat keine Auswirkungen auf
das Telegram Erscheinungsbild

`auth.admin-bot-token=` Token des Admin-Bots

`auth.sender-bot-token=` Token des Sender-Bot

`webhook.external-admin-address=` Die Basisadresse der Webhooks, von
außen auflösbar

`webhook.external-sender-address=${webhook.external-admin-address}`

`webhook.external-admin-port=443`

`webhook.external-sender-port=443`

### Optional
Diese URLs werden an Telegram gesendet um Updates als WebHook zu
erhalten. Sie müssen von außen erreichbar sein (vor Proxies etc.) und
müssen auf `/update` enden.

`webhook.external-admin-url=https://${webhook.external-admin-address}:${webhook.external-admin-port}/update/`

`webhook.external-sender-url=https://${webhook.external-sender-address}:${webhook.external-sender-port}/update/`

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
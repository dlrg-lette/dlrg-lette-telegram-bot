# Telegram Bot - DLRG Lette e. V.
Es handelt sich hierbei um einen Telegram-Bot der DLRG Lette 
für den schnellen Informationsaustausch.

## Funktionen Benutzer:
- Abonnieren von verschiedenen Kategorien
- Ändern des Abo-Status der Kategorien
- Vergessen werden (sämtliche gespeicherten Daten werden entfernt)

## Funktionen Administratoren / Moderatoren:
- Senden einer Nachricht an alle Abonnenten einer bestimmten Kategorie


# Benötigte Komponenten
- MonoDB
- 2 Telegram Bots (bzw. Token)
    - AdminBot -> Für Moderatoren und Admins
    - SenderBot -> Für die Anwender / Mitglieder
    
# Einrichtung
## MonoDB
- Konfiguration der MonoDB über `spring.data.mongodb.*` Parameter.
- Eine MonoDB Datenbank mit folgenden Collections:
    - `adminChat`
    - `category`
    - `senderChat`
    - `user`
    - `texts`

Bis auf `texts` sind alle collections leer
### Initaldaten `texts` collection
[Initialdaten der `texts` collection im JSON Format](master/texts.collection.initial.json)
# ✦ Powitanie – animowane titles przy wejściu (Paper 26.2 / 26.3)

Po wejściu na serwer gracz widzi animację na titlach:

1. **Witaj!** – *Dzień dobry, Nick!* (po 18:00 *Dobry wieczór*)
2. **Na** – *Godzina: 15:20:43* (zegar tyka na żywo)
3. **Craftopia.PL** – *Obecnie jest X graczy online!*
4. **Craftopia.PL** – *Życzymy Miłej Gry & Dobrej Zabawy!*

Tytuły **wpisują się litera po literze** i **kasują od końca**, a po „Craftopia.PL” przelatuje **fala kolorów** (&e → &6 po białym &f), na koniec napis płynnie zanika.

Animacja leci **raz na gracza do restartu serwera** (lista siedzi w pamięci, restart ją czyści).

## Komendy (`powitanie.admin`)
- `/powitanie pokaz [gracz]` – odtwórz animację (test)
- `/powitanie reset` – każdy zobaczy ją ponownie przy następnym wejściu
- `/powitanie reload` – przeładuj config

## Config
Wszystkie teksty, kolory (`&a`, `&6`, hex `&#ffaa00`, gradienty `{gradient:#36d1dc:#5b86e5}tekst{/gradient}`), tempo, kolory fali, pory powitań i kolejne ekrany ustawiasz w `config.yml`.
Placeholdery: `{player}`, `{greeting}`, `{time}`, `{date}`, `{online}`, `{max}`.
